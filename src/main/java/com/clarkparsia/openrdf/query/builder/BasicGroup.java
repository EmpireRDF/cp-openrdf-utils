/*
 * Copyright (c) 2009-2010 Clark & Parsia, LLC. <http://www.clarkparsia.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clarkparsia.openrdf.query.builder;

import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.ValueExpr;
import org.openrdf.query.algebra.Filter;
import org.openrdf.query.algebra.And;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.EmptySet;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.google.common.collect.Collections2;
import com.google.common.base.Predicate;
import com.google.common.base.Function;

/**
 * <p>Internal class for representing a group within a query.</p>
 *
 * @author Michael Grove
 * @since 0.2
 * @version 0.3
 */
public class BasicGroup implements Group {
	private boolean mIsOptional = false;
	private Collection<TupleExpr> mExpressions = new LinkedHashSet<TupleExpr>();
	private List<Group> mChildren = new ArrayList<Group>();
	private Collection<ValueExpr> mFilters = new LinkedHashSet<ValueExpr>();

	/**
	 * Create a new BasicGroup
	 * @param theOptional whether or not the patterns and filters in this group are optional
	 */
	public BasicGroup(final boolean theOptional) {
		mIsOptional = theOptional;
	}

	/**
	 * @inheritDoc
	 */
	public int size() {
		int aSize = mExpressions.size();

		for (Group aChild : mChildren) {
			aSize += aChild.size();
		}

		return aSize;
	}

	/**
	 * @inheritDoc
	 */
	public void addChild(Group theGroup) {
		mChildren.add(theGroup);
	}

	/**
	 * Remove a child from this group
	 * @param theGroup the child to remove
	 */
	public void removeChild(Group theGroup) {
		mChildren.remove(theGroup);
	}

	/**
	 * Add a Filter to this group
	 * @param theExpr the value filter to add
	 */
	public void addFilter(ValueExpr theExpr) {
		mFilters.add(theExpr);
	}

	public boolean isEmpty() {
		return mFilters.isEmpty() && mExpressions.isEmpty() && mChildren.isEmpty();
	}

	/**
	 * @inheritDoc
	 */
	public boolean isOptional() {
		return mIsOptional;
	}

	/**
	 * @inheritDoc
	 */
	public TupleExpr expr() {
		return expr(true);
	}

	private TupleExpr expr(boolean filterExpr) {
		TupleExpr aExpr = null;

		if (mExpressions.isEmpty() && mFilters.isEmpty()) {
			if (mChildren.isEmpty()) {
				return null;
			}
		}
		else if (mExpressions.isEmpty() && !mFilters.isEmpty()) {
			if (mChildren.isEmpty()) {
				aExpr = new Filter(new EmptySet(), filtersAsAnd());
			}
		}
		else {
			aExpr = asJoin(mExpressions);

			if (filterExpr) {
				aExpr = filteredTuple(aExpr);
			}
		}

		if (!mChildren.isEmpty()) {
			for (Group aGroup : mChildren) {
				if (aExpr == null) {
					if (mExpressions.isEmpty() && !mFilters.isEmpty()) {
						aExpr = new Filter(aGroup.expr(), filtersAsAnd());
					}
					else {
						aExpr = aGroup.expr();
					}
				}
				else {
					BinaryTupleOperator aJoin = aGroup.isOptional() ? new LeftJoin() : new Join();

					aJoin.setLeftArg(aExpr);

					if (aGroup.isOptional() && aJoin instanceof LeftJoin && aGroup instanceof BasicGroup && !((BasicGroup)aGroup).mFilters.isEmpty()) {

						BasicGroup aBasicGroup = (BasicGroup) aGroup;

						aJoin.setRightArg(aBasicGroup.expr(false));

						((LeftJoin)aJoin).setCondition(aBasicGroup.filtersAsAnd());
					}
					else {
						aJoin.setRightArg(aGroup.expr());
					}

					aExpr = aJoin;
				}

			}
		}
		
		return aExpr;
	}

	private TupleExpr filteredTuple(TupleExpr theExpr) {
		TupleExpr aExpr = theExpr ;

		for (ValueExpr aValEx : mFilters) {
			Filter aFilter = new Filter();
			aFilter.setCondition(aValEx);
			aFilter.setArg(aExpr);
			aExpr = aFilter;
		}

		return aExpr;
	}

	private ValueExpr filtersAsAnd() {
		ValueExpr aExpr = null;

		for (ValueExpr aValEx : mFilters) {
			if (aExpr == null) {
				aExpr = aValEx;
			}
			else {
				And aAnd = new And();
				aAnd.setLeftArg(aValEx);
				aAnd.setRightArg(aExpr);
				aExpr = aAnd;
			}
		}

		return aExpr;
	}

	public void add(final TupleExpr theExpr) {
		mExpressions.add(theExpr);
	}

    public void addAll(final Collection<? extends TupleExpr> theTupleExprs) {
        mExpressions.addAll(theTupleExprs);
    }

	private TupleExpr asJoin(Collection<TupleExpr> theList) {
		Join aJoin = new Join();

		if (theList.isEmpty()) {
			throw new RuntimeException("Can't have an empty or missing join.");
		}
		else if (theList.size() == 1) {
			return theList.iterator().next();
		}

		for (TupleExpr aExpr : theList) {
			if (aJoin.getLeftArg() == null) {
				aJoin.setLeftArg(aExpr);
			}
			else if (aJoin.getRightArg() == null) {
				aJoin.setRightArg(aExpr);
			}
			else {
				Join aNewJoin = new Join();

				aNewJoin.setLeftArg(aJoin);
				aNewJoin.setRightArg(aExpr);

				aJoin = aNewJoin;
			}
		}

		return aJoin;
	}

	public Collection<StatementPattern> getPatterns() {
		return Collections2.transform(Collections2.filter(mExpressions, new Predicate<TupleExpr>() { public boolean apply(TupleExpr theExpr) { return theExpr instanceof StatementPattern; } }),
									  new Cast<TupleExpr, StatementPattern>(StatementPattern.class));
	}

	/**
	 * A function that takes an object of one type and casts it to another.
	 * <code><pre>
	 *   Collection&lt;Integer&gt; aNewCollection = transform(aCollection, new Cast&lt;Number, Integer&gt;());
	 * </pre></code>
	 * @param <I> the input type
	 * @param <O> the output type
	 */
	public static class Cast<I, O> implements Function<I, O> {
		private Class<O> mClass;

		public Cast(final Class<O> theClass) {
			mClass = theClass;
		}

		public O apply(final I theIn) {
			return mClass.cast(theIn);
		}
	}
}

/*
 * Copyright (c) 2009-2012 Clark & Parsia, LLC. <http://www.clarkparsia.com>
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

package com.clarkparsia.openrdf;

import java.util.Collection;

import com.google.common.collect.Constraint;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * <p>A {@link Graph} which has a {@link Constraint} placed upon which statements can be added to the Graph.</p>
 *
 * @author Michael Grove
 * @since	0.8
 * @version	0.8
 */
public final class ConstrainedGraph extends DelegatingGraph {
	private final Constraint<Statement> mConstraint;

	ConstrainedGraph(final Graph theGraph, final Constraint<Statement> theConstraint) {
		super(theGraph);
		mConstraint = theConstraint;
	}

	/**
	 * Create a new empty, ConstrainedGraph, which will have the specified constraint place on all additions
	 *
	 * @param theConstraint	the constraint to enforce
	 * @return				the new ConstrainedGraph
	 */
	public static ConstrainedGraph of(final Constraint<Statement> theConstraint) {
		return of(new SetGraph(), theConstraint);
	}

	/**
	 * Create a new ConstrainedGraph which will have the specified constraint enforced on all additions.  Does not
	 * retroactively enforce the constraint, so the provided Graph can contain invalid elements.
	 *
	 * @param theGraph			the graph to constrain
	 * @param theConstraint		the constraint to enforce
	 * @return					the new ConstrainedGraph
	 */
	public static ConstrainedGraph of(final Graph theGraph, final Constraint<Statement> theConstraint) {
		return new ConstrainedGraph(theGraph, theConstraint);
	}

	/**
	 * Return a {@link Constraint} which will only allow {@link OpenRdfUtil#isLiteralValid(org.openrdf.model.Literal) valid} literals into the graph.
	 *
	 * @return	a Constraint to enforce valid literals
	 */
	public static Constraint<Statement> onlyValidLiterals() {
		return new Constraint<Statement>() {
			@Override
			public Statement checkElement(final Statement theStatement) {
				if (theStatement.getObject() instanceof Literal && !OpenRdfUtil.isLiteralValid((Literal) theStatement.getObject())) {
					throw new StatementViolatedConstraintException(theStatement.getObject() + " is not a well-formed literal value.");
				}

				return theStatement;
			}
		};
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean add(final Statement e) {
		mConstraint.checkElement(e);

		return super.add(e);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean add(final Resource theResource, final URI theURI, final Value theValue, final Resource... theContexts) {

		if (theContexts == null || theContexts.length == 0) {
			return add(getValueFactory().createStatement(theResource, theURI, theValue));
		}
		else {
			boolean aAdded = true;
			for (Resource aCxt : theContexts) {
				aAdded |= add(getValueFactory().createStatement(theResource, theURI, theValue, aCxt));
			}

			return aAdded;
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean addAll(final Collection<? extends Statement> c) {
		all(c, mConstraint);

		return super.addAll(c);
	}

	private static <T> void all(final Iterable<? extends T> theObjects, final Constraint<T> theConstraint) {
		for (T aObj : theObjects) {
			theConstraint.checkElement(aObj);
		}
	}

	/**
	 * A runtime exception suitable for being thrown from a {@link Constraint} on a {@link Statement}
	 */
	public static class StatementViolatedConstraintException extends RuntimeException {

		/**
		 * Create a new StatementViolatedConstraintException
		 * @param theMessage	a note about why the constraint was violated
		 */
		public StatementViolatedConstraintException(final String theMessage) {
			super(theMessage);
		}
	}
}

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

import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedBooleanQuery;

import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.SameTerm;
import org.openrdf.query.algebra.ValueConstant;

import org.openrdf.model.Resource;

/**
 * <p>Factory class for obtaining instances of {@link QueryBuilder} objects for the various types of queries.</p>
 *
 * @author Michael Grove
 * @since 0.2
 * @since 0.3.1
 */
public class QueryBuilderFactory {
    /**
     * Create a QueryBuilder for creating a select query
     * @return a select QueryBuilder
     */
    public static QueryBuilder<ParsedBooleanQuery> ask() {
        return new AbstractQueryBuilder<ParsedBooleanQuery>(new ParsedBooleanQuery());
    }

    /**
     * Create a QueryBuilder for creating a select query
     * @return a select QueryBuilder
     */
    public static QueryBuilder<ParsedTupleQuery> select() {
        return new AbstractQueryBuilder<ParsedTupleQuery>(new ParsedTupleQuery());
    }

    /**
     * Create a QueryBuilder for creating a select query
     * @param theProjectionVars the list of elements in the projection of the query
     * @return a select query builder
     */
    public static QueryBuilder<ParsedTupleQuery> select(String... theProjectionVars) {
        QueryBuilder<ParsedTupleQuery> aBuilder = new AbstractQueryBuilder<ParsedTupleQuery>(new ParsedTupleQuery());
        aBuilder.addProjectionVar(theProjectionVars);

        return aBuilder;
    }

    /**
     * Create a QueryBuilder for building a construct query
     * @return a construct QueryBuilder
     */
    public static QueryBuilder<ParsedGraphQuery> construct() {
        return new AbstractQueryBuilder<ParsedGraphQuery>(new ParsedGraphQuery());
    }

	/**
	 * Create a QueryBuilder for creating a describe query
	 * @param theValues the specific bound URI values to be described
	 * @return a describe query builder
	 */
	public static QueryBuilder<ParsedGraphQuery> describe(Resource... theValues) {
		return describe(null, theValues);
	}

	/**
	 * Create a QueryBuilder for creating a describe query
	 * @param theVars the variables to be described
	 * @param theValues the specific bound URI values to be described
	 * @return a describe query builder
	 */
	public static QueryBuilder<ParsedGraphQuery> describe(String[] theVars, Resource... theValues) {
		QueryBuilder<ParsedGraphQuery> aBuilder = new AbstractQueryBuilder<ParsedGraphQuery>(new ParsedGraphQuery());

		aBuilder.reduced();
		aBuilder.addProjectionVar("descr_subj", "descr_pred", "descr_obj");
		GroupBuilder<?,?> aGroup = aBuilder.group();

		if (theVars != null) {
			for (String aVar : theVars) {
				Var aVarObj = new Var(aVar);
				aVarObj.setAnonymous(true);

				aGroup.filter().or(new SameTerm(aVarObj, new Var("descr_subj")),
								   new SameTerm(aVarObj, new Var("descr_obj")));
			}
		}

		if (theValues != null) {
			for (Resource aVar : theValues) {
				Var aSubjVar = new Var("descr_subj");
				aSubjVar.setAnonymous(true);

				Var aObjVar = new Var("descr_obj");
				aObjVar.setAnonymous(true);
				
				aGroup.filter().or(new SameTerm(new ValueConstant(aVar), aSubjVar),
					 			   new SameTerm(new ValueConstant(aVar), aObjVar));
			}
		}

		aGroup.atom("descr_subj", "descr_pred", "descr_obj");

		return aBuilder;
	}
}

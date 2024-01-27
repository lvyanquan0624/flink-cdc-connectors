/*
 * Copyright 2023 Ververica Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.cdc.runtime.parser;

import org.apache.flink.api.common.io.ParseException;

import com.ververica.cdc.common.utils.StringUtils;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.Java;

import java.util.ArrayList;
import java.util.List;

/** Use Janino parser to parse the statement of flink cdc pipeline transform. */
public class JaninoParser {

    public static String loadSystemFunction(String expression) {
        return "import static com.ververica.cdc.runtime.functions.SystemFunctionUtils.*;"
                + expression;
    }

    public static String translateSqlNodeToJaninoExpression(SqlBasicCall transform) {
        Java.Rvalue rvalue = translateJaninoAST(transform);
        return rvalue.toString();
    }

    private static Java.Rvalue translateJaninoAST(SqlBasicCall sqlBasicCall) {
        List<SqlNode> operandList = sqlBasicCall.getOperandList();
        List<Java.Rvalue> atoms = new ArrayList<>();
        for (SqlNode sqlNode : operandList) {
            if (sqlNode instanceof SqlIdentifier) {
                SqlIdentifier sqlIdentifier = (SqlIdentifier) sqlNode;
                String columnName = sqlIdentifier.names.get(sqlIdentifier.names.size() - 1);
                atoms.add(new Java.AmbiguousName(Location.NOWHERE, new String[] {columnName}));
            } else if (sqlNode instanceof SqlLiteral) {
                SqlLiteral sqlLiteral = (SqlLiteral) sqlNode;
                String value = sqlLiteral.getValue().toString();
                if (sqlLiteral instanceof SqlCharStringLiteral) {
                    value = "String.valueOf(" + value + ")";
                }
                atoms.add(new Java.AmbiguousName(Location.NOWHERE, new String[] {value}));
            } else if (sqlNode instanceof SqlBasicCall) {
                atoms.add(translateJaninoAST((SqlBasicCall) sqlNode));
            }
        }
        return sqlBasicCallToJaninoRvalue(sqlBasicCall, atoms.toArray(new Java.Rvalue[0]));
    }

    private static Java.Rvalue sqlBasicCallToJaninoRvalue(
            SqlBasicCall sqlBasicCall, Java.Rvalue[] atoms) {
        switch (sqlBasicCall.getKind()) {
            case AND:
                return generateBinaryOperation(sqlBasicCall, atoms, "&&");
            case OR:
                return generateBinaryOperation(sqlBasicCall, atoms, "||");
            case NOT:
                return generateUnaryOperation("!", atoms[0]);
            case EQUALS:
                return generateEqualsOperation(sqlBasicCall, atoms);
            case NOT_EQUALS:
                return generateUnaryOperation("!", generateEqualsOperation(sqlBasicCall, atoms));
            case IS_NULL:
                return generateUnaryOperation("null == ", atoms[0]);
            case IS_NOT_NULL:
                return generateUnaryOperation("null != ", atoms[0]);
            case IS_FALSE:
            case IS_NOT_TRUE:
                return generateUnaryOperation("false == ", atoms[0]);
            case IS_TRUE:
            case IS_NOT_FALSE:
                return generateUnaryOperation("true == ", atoms[0]);
            case BETWEEN:
            case LIKE:
            case CEIL:
            case FLOOR:
            case OTHER_FUNCTION:
                return new Java.MethodInvocation(
                        Location.NOWHERE,
                        null,
                        StringUtils.convertToCamelCase(sqlBasicCall.getOperator().getName()),
                        atoms);
            case PLUS:
                return generateBinaryOperation(sqlBasicCall, atoms, "+");
            case MINUS:
                return generateBinaryOperation(sqlBasicCall, atoms, "-");
            case TIMES:
                return generateBinaryOperation(sqlBasicCall, atoms, "*");
            case DIVIDE:
                return generateBinaryOperation(sqlBasicCall, atoms, "/");
            case MOD:
                return generateBinaryOperation(sqlBasicCall, atoms, "%");
            case LESS_THAN:
            case GREATER_THAN:
            case LESS_THAN_OR_EQUAL:
            case GREATER_THAN_OR_EQUAL:
                return generateBinaryOperation(sqlBasicCall, atoms, sqlBasicCall.getKind().sql);
            case OTHER:
                return generateOtherOperation(sqlBasicCall, atoms);
            default:
                throw new ParseException("Unrecognized expression: " + sqlBasicCall.toString());
        }
    }

    private static Java.Rvalue generateUnaryOperation(String operator, Java.Rvalue atom) {
        return new Java.UnaryOperation(Location.NOWHERE, operator, atom);
    }

    private static Java.Rvalue generateBinaryOperation(
            SqlBasicCall sqlBasicCall, Java.Rvalue[] atoms, String operator) {
        if (atoms.length != 2) {
            throw new ParseException("Unrecognized expression: " + sqlBasicCall.toString());
        }
        return new Java.BinaryOperation(Location.NOWHERE, atoms[0], operator, atoms[1]);
    }

    private static Java.Rvalue generateEqualsOperation(
            SqlBasicCall sqlBasicCall, Java.Rvalue[] atoms) {
        if (atoms.length != 2) {
            throw new ParseException("Unrecognized expression: " + sqlBasicCall.toString());
        }
        if (atoms[0] instanceof Java.AmbiguousName) {
            if (atoms[0].toString().contains("String.valueOf(")) {
                return new Java.MethodInvocation(
                        Location.NOWHERE, atoms[0], "equals", new Java.Rvalue[] {atoms[1]});
            } else if (atoms[1].toString().contains("String.valueOf(")) {
                return new Java.MethodInvocation(
                        Location.NOWHERE, atoms[1], "equals", new Java.Rvalue[] {atoms[0]});
            }
        }
        return new Java.BinaryOperation(Location.NOWHERE, atoms[0], "==", atoms[1]);
    }

    private static Java.Rvalue generateOtherOperation(
            SqlBasicCall sqlBasicCall, Java.Rvalue[] atoms) {
        if (sqlBasicCall.getOperator().getName().equals("||")) {
            return new Java.MethodInvocation(
                    Location.NOWHERE, null, StringUtils.convertToCamelCase("CONCAT"), atoms);
        }
        throw new ParseException("Unrecognized expression: " + sqlBasicCall.toString());
    }
}
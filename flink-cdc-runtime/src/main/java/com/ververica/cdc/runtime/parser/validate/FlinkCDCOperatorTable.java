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

package com.ververica.cdc.runtime.parser.validate;

import com.ververica.cdc.runtime.functions.BuiltInSqlFunction;
import com.ververica.cdc.runtime.functions.FlinkCDCTimestampFunction;
import com.ververica.cdc.runtime.functions.FlinkReturnTypes;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlOperandCountRanges;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.type.SqlTypeTransforms;
import org.apache.calcite.sql.util.ReflectiveSqlOperatorTable;
import org.apache.calcite.sql.validate.SqlNameMatcher;
import org.apache.calcite.sql.validate.SqlNameMatchers;

import javax.annotation.Nullable;

import java.util.List;

/** FlinkCDCOperatorTable to generate the metadata of calcite. */
public class FlinkCDCOperatorTable extends ReflectiveSqlOperatorTable {

    private static FlinkCDCOperatorTable instance;

    private FlinkCDCOperatorTable() {}

    public static synchronized FlinkCDCOperatorTable instance() {
        if (instance == null) {
            instance = new FlinkCDCOperatorTable();
            instance.init();
        }

        return instance;
    }

    @Override
    public void lookupOperatorOverloads(
            SqlIdentifier opName,
            @Nullable SqlFunctionCategory sqlFunctionCategory,
            SqlSyntax syntax,
            List<SqlOperator> operatorList,
            SqlNameMatcher nameMatcher) {
        // set caseSensitive=false to make sure the behavior is same with before.
        super.lookupOperatorOverloads(
                opName,
                sqlFunctionCategory,
                syntax,
                operatorList,
                SqlNameMatchers.withCaseSensitive(false));
    }

    public static final SqlFunction CONCAT_FUNCTION =
            BuiltInSqlFunction.newBuilder()
                    .name("CONCAT")
                    .returnType(
                            ReturnTypes.cascade(
                                    ReturnTypes.explicit(SqlTypeName.VARCHAR),
                                    SqlTypeTransforms.TO_NULLABLE))
                    .operandTypeChecker(
                            OperandTypes.repeat(SqlOperandCountRanges.from(1), OperandTypes.STRING))
                    .build();
    public static final SqlFunction LOCALTIMESTAMP =
            new FlinkCDCTimestampFunction("LOCALTIMESTAMP", SqlTypeName.TIMESTAMP, 3);
    public static final SqlFunction CURRENT_TIMESTAMP =
            new FlinkCDCTimestampFunction(
                    "CURRENT_TIMESTAMP", SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE, 3);
    public static final SqlFunction CURRENT_DATE =
            new FlinkCDCTimestampFunction("CURRENT_DATE", SqlTypeName.DATE, 0);
    public static final SqlFunction NOW =
            new FlinkCDCTimestampFunction("NOW", SqlTypeName.TIMESTAMP_WITH_LOCAL_TIME_ZONE, 3) {
                @Override
                public SqlSyntax getSyntax() {
                    return SqlSyntax.FUNCTION;
                }
            };
    public static final SqlFunction TO_DATE =
            new SqlFunction(
                    "TO_DATE",
                    SqlKind.OTHER_FUNCTION,
                    ReturnTypes.cascade(
                            ReturnTypes.explicit(SqlTypeName.DATE),
                            SqlTypeTransforms.FORCE_NULLABLE),
                    null,
                    OperandTypes.or(
                            OperandTypes.family(SqlTypeFamily.STRING),
                            OperandTypes.family(SqlTypeFamily.STRING, SqlTypeFamily.STRING)),
                    SqlFunctionCategory.TIMEDATE);
    public static final SqlFunction TO_TIMESTAMP =
            new SqlFunction(
                    "TO_TIMESTAMP",
                    SqlKind.OTHER_FUNCTION,
                    ReturnTypes.cascade(
                            ReturnTypes.explicit(SqlTypeName.TIMESTAMP, 3),
                            SqlTypeTransforms.FORCE_NULLABLE),
                    null,
                    OperandTypes.or(
                            OperandTypes.family(SqlTypeFamily.CHARACTER),
                            OperandTypes.family(SqlTypeFamily.CHARACTER, SqlTypeFamily.CHARACTER)),
                    SqlFunctionCategory.TIMEDATE);
    public static final SqlFunction REGEXP_REPLACE =
            new SqlFunction(
                    "REGEXP_REPLACE",
                    SqlKind.OTHER_FUNCTION,
                    ReturnTypes.cascade(
                            ReturnTypes.explicit(SqlTypeName.VARCHAR),
                            SqlTypeTransforms.TO_NULLABLE),
                    null,
                    OperandTypes.family(
                            SqlTypeFamily.STRING, SqlTypeFamily.STRING, SqlTypeFamily.STRING),
                    SqlFunctionCategory.STRING);
    public static final SqlFunction SUBSTR =
            new SqlFunction(
                    "SUBSTR",
                    SqlKind.OTHER_FUNCTION,
                    FlinkReturnTypes.ARG0_VARCHAR_FORCE_NULLABLE,
                    null,
                    OperandTypes.or(
                            OperandTypes.family(SqlTypeFamily.CHARACTER, SqlTypeFamily.INTEGER),
                            OperandTypes.family(
                                    SqlTypeFamily.CHARACTER,
                                    SqlTypeFamily.INTEGER,
                                    SqlTypeFamily.INTEGER)),
                    SqlFunctionCategory.STRING);
    public static final SqlFunction ROUND =
            new SqlFunction(
                    "ROUND",
                    SqlKind.OTHER_FUNCTION,
                    FlinkReturnTypes.ROUND_FUNCTION_NULLABLE,
                    null,
                    OperandTypes.or(OperandTypes.NUMERIC_INTEGER, OperandTypes.NUMERIC),
                    SqlFunctionCategory.NUMERIC);
    public static final SqlFunction UUID =
            BuiltInSqlFunction.newBuilder()
                    .name("UUID")
                    .returnType(ReturnTypes.explicit(SqlTypeName.CHAR, 36))
                    .operandTypeChecker(OperandTypes.NILADIC)
                    .notDeterministic()
                    .build();
    public static final SqlFunction MOD = SqlStdOperatorTable.MOD;
    public static final SqlFunction LOCALTIME = SqlStdOperatorTable.LOCALTIME;
    public static final SqlFunction YEAR = SqlStdOperatorTable.YEAR;
    public static final SqlFunction QUARTER = SqlStdOperatorTable.QUARTER;
    public static final SqlFunction MONTH = SqlStdOperatorTable.MONTH;
    public static final SqlFunction WEEK = SqlStdOperatorTable.WEEK;
    public static final SqlFunction TIMESTAMP_ADD = SqlStdOperatorTable.TIMESTAMP_ADD;
    public static final SqlFunction TIMESTAMP_DIFF = SqlStdOperatorTable.TIMESTAMP_DIFF;
    public static final SqlOperator BETWEEN = SqlStdOperatorTable.BETWEEN;
    public static final SqlOperator SYMMETRIC_BETWEEN = SqlStdOperatorTable.SYMMETRIC_BETWEEN;
    public static final SqlOperator NOT_BETWEEN = SqlStdOperatorTable.NOT_BETWEEN;
    public static final SqlOperator IN = SqlStdOperatorTable.IN;
    public static final SqlOperator NOT_IN = SqlStdOperatorTable.NOT_IN;
    public static final SqlFunction CHAR_LENGTH = SqlStdOperatorTable.CHAR_LENGTH;
    public static final SqlFunction TRIM = SqlStdOperatorTable.TRIM;
    public static final SqlOperator NOT_LIKE = SqlStdOperatorTable.NOT_LIKE;
    public static final SqlOperator LIKE = SqlStdOperatorTable.LIKE;
    public static final SqlFunction UPPER = SqlStdOperatorTable.UPPER;
    public static final SqlFunction LOWER = SqlStdOperatorTable.LOWER;
    public static final SqlFunction ABS = SqlStdOperatorTable.ABS;
    public static final SqlFunction NULLIF = SqlStdOperatorTable.NULLIF;
    public static final SqlFunction FLOOR = SqlStdOperatorTable.FLOOR;
    public static final SqlFunction CEIL = SqlStdOperatorTable.CEIL;
}

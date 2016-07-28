package info.smart_tools.smartactors.core.postgres_schema.search;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(Operators.class)
//public class OperatorsTest {
//    private ConditionsResolverBase conditionsResolver;
//
//    @Before
//    public void setUp() {
//        conditionsResolver = ConditionsWriterResolver.create();
//    }
//
//    @Test
//    public void should_AddsAllOperators() throws Exception {
//        ConditionsResolverBase conditionsResolverBase = mock(ConditionsResolverBase.class);
//        Operators.addAll(conditionsResolverBase);
//
//        verify(conditionsResolverBase, times(12)).addOperator(anyString(), anyObject());
//        verifyPrivate(Operators.class, times(10)).invoke("formattedCheckWriter", anyString());
//    }
//
//    @Test
//    public void writeFieldCheckConditionTest() throws Exception {
//        final int OPERATORS_NUMBER = 10;
//        final String queryParam = "testQueryParam";
//
//        List<String> operatorsNames = new ArrayList<>(OPERATORS_NUMBER);
//        List<FieldPath> fieldsPaths = new ArrayList<>(OPERATORS_NUMBER);
//        List<SQLQueryParameterSetter> setters = new ArrayList<>(OPERATORS_NUMBER);
//        List<String> result = new ArrayList<>(OPERATORS_NUMBER);
//
//        operatorsNames.addAll(
//                Arrays.asList(
//                        "$eq", "$ne", "$lt",
//                        "$gt", "$lte", "$gte",
//                        "$date-from", "$date-to",
//                        "$hasTag", "$fulltext"));
//
//        for (String name : operatorsNames)
//                fieldsPaths.add(PostgresFieldPath.fromString(name.replace("$", "")));
//
//        result.addAll(
//                Arrays.asList(
//                        "((document#>'{eq}')=to_json(?)::jsonb)", "((document#>'{ne}')!=to_json(?)::jsonb)",
//                        "((document#>'{lt}')<to_json(?)::jsonb)", "((document#>'{gt}')>to_json(?)::jsonb)",
//                        "((document#>'{lte}')<=to_json(?)::jsonb)", "((document#>'{gte}')>=to_json(?)::jsonb)",
//                        "(parse_timestamp_immutable(document#>'{date-from}')>=(?)::timestamp)",
//                        "(parse_timestamp_immutable(document#>'{date-to}')<=(?)::timestamp)",
//                        "((document#>'{hasTag}')??(?))",
//                        "(to_tsvector('russian',(document#>'{fulltext}')::text))@@(to_tsquery(russian,?))"));
//
//        for (int i = 0; i < OPERATORS_NUMBER; ++i) {
//            QueryStatement queryStatement = new QueryStatement();
//            conditionsResolver
//                    .resolve(operatorsNames.get(i))
//                    .write(queryStatement, conditionsResolver, fieldsPaths.get(i), queryParam, setters);
//            assertEquals(queryStatement.getBodyWriter().toString().trim(), result.get(i));
//        }
//        assertEquals(setters.size(), 10);
//    }
//
//    @Test
//    public void writeFieldExistsCheckConditionTest() throws Exception {
//        final int OPERATORS_NUMBER = 1;
//
//        List<SQLQueryParameterSetter> setters = new ArrayList<>(OPERATORS_NUMBER);
//        FieldPath fieldPath = PostgresFieldPath.fromString("isNull");
//
//        QueryStatement queryStatementIsNull = new QueryStatement();
//        conditionsResolver
//                .resolve("$isNull")
//                .write(queryStatementIsNull, conditionsResolver, fieldPath, "true", setters);
//        assertEquals(queryStatementIsNull.getBodyWriter().toString().trim(), "(document#>'{isNull}') is null");
//
//        QueryStatement queryStatementIsNotNull = new QueryStatement();
//        conditionsResolver
//                .resolve("$isNull")
//                .write(queryStatementIsNotNull, conditionsResolver, fieldPath, "false", setters);
//        assertEquals(queryStatementIsNotNull.getBodyWriter().toString().trim(), "(document#>'{isNull}') is not null");
//    }
//
//    @Test
//    public void writeFieldInArrayCheckConditionTest() throws Exception {
//        final String operatorName = "$in";
//        final int OPERATORS_NUMBER = 1;
//
//        List<SQLQueryParameterSetter> setters = new ArrayList<>(OPERATORS_NUMBER);
//        FieldPath fieldPath = PostgresFieldPath.fromString(operatorName.replace("$", ""));
//        List<Integer> queryParam = new LinkedList<>(Arrays.asList(1, 10, 100));
//
//        QueryStatement queryStatement = new QueryStatement();
//        conditionsResolver
//                .resolve(operatorName)
//                .write(queryStatement, conditionsResolver, fieldPath, queryParam, setters);
//        assertEquals(queryStatement.getBodyWriter().toString().trim(),
//                "((document#>'{in}')in(to_json(?)::jsonb,to_json(?)::jsonb,to_json(?)::jsonb))");
//        assertEquals(setters.size(), 1);
//    }
//
//    @Test(expected = QueryBuildException.class)
//    public void writeFieldCheckCondition_ContextFieldPath_IsNull_Test() throws QueryBuildException {
//        conditionsResolver
//                .resolve("$eq")
//                .write(new QueryStatement(), conditionsResolver, null, "param", new LinkedList<>());
//    }
//
//    @Test(expected = QueryBuildException.class)
//    public void writeFieldExistsCheckCondition_ContextFieldPath_IsNull_Test() throws QueryBuildException {
//        conditionsResolver
//                .resolve("$isNull")
//                .write(new QueryStatement(), conditionsResolver, null, "true", new LinkedList<>());
//    }
//
//    @Test(expected = QueryBuildException.class)
//    public void writeFieldExistsCheckCondition_QueryParameter_Invalid_Test() throws QueryBuildException {
//        conditionsResolver
//                .resolve("$isNull")
//                .write(new QueryStatement(), conditionsResolver,
//                        PostgresFieldPath.fromString("fieldPath"), "invalidParam", new LinkedList<>());
//    }
//
//    @Test(expected = QueryBuildException.class)
//    public void writeFieldInArrayCheckCondition_ContextFieldPath_IsNull_Test() throws QueryBuildException {
//        conditionsResolver
//                .resolve("$in")
//                .write(new QueryStatement(), conditionsResolver, null, "param", new LinkedList<>());
//    }
//
//    @Test(expected = QueryBuildException.class)
//    public void writeFieldInArrayCheckCondition_QueryParameter_Invalid_Test() throws QueryBuildException {
//        conditionsResolver
//                .resolve("$in")
//                .write(new QueryStatement(), conditionsResolver,
//                        PostgresFieldPath.fromString("fieldPath"), "invalidParam", new LinkedList<>());
//    }
//}

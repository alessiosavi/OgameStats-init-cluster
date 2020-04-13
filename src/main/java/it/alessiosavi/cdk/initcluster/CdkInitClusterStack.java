package it.alessiosavi.cdk.initcluster;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CdkInitClusterStack extends Stack {
    public CdkInitClusterStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CdkInitClusterStack(final Construct scope, final String id,
                               final StackProps props) {
        super(scope, id, props);

        Table playerData = CreatePlayerDataTable("PlayerData");
        Function lambdaStats = CreateLambdaFunctionStats(playerData.getTableName());
        Function lambdaUpdater = CreateLambdaFunctionUpdater(playerData.getTableName(), "166");

        playerData.grantReadData(lambdaStats);
        playerData.grantReadWriteData(lambdaUpdater);

        RestApi api = new RestApi(this, "itemsApi",
                RestApiProps.builder().restApiName("Items Service").build());

        api.getRoot().addResource("v1");
        api.getRoot().addMethod("POST", new LambdaIntegration(lambdaStats));
        addCorsOptions(api);

//        RestApi restApiStats = createRestApiStats(lambdaStats);
//        RestApi api = createRestApiUpdater(lambdaUpdater);
    }


    private void addCorsOptions(RestApi api) {
        List<MethodResponse> methoedResponses = new ArrayList<>();

        Map<String, Boolean> responseParameters = new HashMap<>();
        responseParameters.put("method.response.header.Access-Control-Allow-Headers", Boolean.TRUE);
        responseParameters.put("method.response.header.Access-Control-Allow-Methods", Boolean.TRUE);
        responseParameters.put("method.response.header.Access-Control-Allow-Credentials", Boolean.TRUE);
        responseParameters.put("method.response.header.Access-Control-Allow-Origin", Boolean.TRUE);
        methoedResponses.add(MethodResponse.builder()
                .responseParameters(responseParameters)
                .statusCode("200")
                .build());
        MethodOptions methodOptions = MethodOptions.builder()
                .methodResponses(methoedResponses)
                .build();

        Map<String, String> requestTemplate = new HashMap<>();
        requestTemplate.put("application/json", "{\"statusCode\": 200}");
        List<IntegrationResponse> integrationResponses = new ArrayList<>();

        Map<String, String> integrationResponseParameters = new HashMap<>();
        integrationResponseParameters.put("method.response.header.Access-Control-Allow-Headers", "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token,X-Amz-User-Agent'");
        integrationResponseParameters.put("method.response.header.Access-Control-Allow-Origin", "'*'");
        integrationResponseParameters.put("method.response.header.Access-Control-Allow-Credentials", "'false'");
        integrationResponseParameters.put("method.response.header.Access-Control-Allow-Methods", "'OPTIONS,GET,PUT,POST,DELETE'");
        integrationResponses.add(IntegrationResponse.builder()
                .responseParameters(integrationResponseParameters)
                .statusCode("200")
                .build());

        Integration methodIntegration = MockIntegration.Builder.create()
                .integrationResponses(integrationResponses)
                .passthroughBehavior(PassthroughBehavior.NEVER)
                .requestTemplates(requestTemplate)
                .build();

        api.getRoot().addMethod("OPTIONS", methodIntegration, methodOptions);
    }


    private LambdaRestApi createRestApiStats(Function lambda) {

        return LambdaRestApi.Builder.create(this, "OgameStatsREST")
                .restApiName("OgameStatsREST")
                .description("This service is delegated to compute the candidate attack")
                .handler(lambda)
                .proxy(false)
                .build();
    }


    private RestApi createRestApiUpdater(Function lambda) {
        LambdaRestApi api = LambdaRestApi.Builder.create(this, "PopulatePlayerDBREST")
                .restApiName("PopulatePlayerDBRestApi")
                .description("This service is delegated to populate the DynamoDB with all the data related to the players.")
                .handler(lambda)
                .proxy(false)
                .build();

        Rule rule = Rule.Builder.create(this, "PopulatePlayerDBREST_CRON")
                .description("Run every Monday")
                .schedule(Schedule.expression("cron(0 12 ? * MON *)"))
                .build();

        rule.addTarget(new LambdaFunction(lambda));
        return api;
    }

    private Function CreateLambdaFunctionUpdater(String tableName, String uni) {
        Map<String, String> env = new HashMap<>();
        env.put("table_name", tableName);
        env.put("uni", uni);
        return Function.Builder.create(this, "PopulatePlayerDB")
                .code(Code.fromAsset(Costants.OgameSyncBINPath))
                .handler("PopulatePlayerDB")
                .timeout(Duration.seconds(128))
                .functionName("PopulatePlayerDB")
                .runtime(Runtime.GO_1_X)
                .environment(env)
                .memorySize(3 * 60) // 3 minutes
                .retryAttempts(1)
                .build();
    }

    private Table CreatePlayerDataTable(String TABLE_NAME) {
        return Table.Builder.create(this, TABLE_NAME)
                .tableName(TABLE_NAME)
                .partitionKey(Attribute.builder().type(AttributeType.STRING).name("ID").build())
                .sortKey(Attribute.builder()
                        .type(AttributeType.STRING)
                        .name("Username")
                        .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private Function CreateLambdaFunctionStats(String tableName) {
        Map<String, String> env = new HashMap<>();
        env.put("table_name", tableName);
        return Function.Builder.create(this, "OgameStatsLambda")
                .code(Code.fromAsset(Costants.OgameStatsBINPath))
                .handler("GoStatOgame")
                .timeout(Duration.seconds(10))
                .functionName("OgameStatsLambda")
                .runtime(Runtime.GO_1_X)
                .environment(env)
                .memorySize(128)
                .retryAttempts(1)
                .build();
    }
}

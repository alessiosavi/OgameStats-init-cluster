package it.alessiosavi.cdk.initcluster;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.HashMap;
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

        RestApi api = createRestApi(lambdaUpdater);


        Rule rule = Rule.Builder.create(this, "PopulatePlayerDBREST_CRON")
                .description("Run every Monday")
                .schedule(Schedule.expression("cron(0 12 ? * MON *)"))
                .build();

        rule.addTarget(new LambdaFunction(lambdaUpdater));

    }

    private RestApi createRestApi(Function lambda) {
        RestApi api = RestApi.Builder.create(this, "PopulatePlayerDBREST")
                .restApiName("PopulatePlayerDBRestApi")
                .description("This service is delegated to populate the DynamoDB with all the data related to the players.")
                .build();
        LambdaIntegration ogameStatsIntegration = LambdaIntegration.Builder.create(lambda)
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }})
                .build();
        api.getRoot().addMethod("GET", ogameStatsIntegration);
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
                .timeout(Duration.seconds(30))
                .functionName("OgameStatsLambda")
                .runtime(Runtime.GO_1_X)
                .environment(env)
                .memorySize(300)
                .retryAttempts(1)
                .build();
    }
}

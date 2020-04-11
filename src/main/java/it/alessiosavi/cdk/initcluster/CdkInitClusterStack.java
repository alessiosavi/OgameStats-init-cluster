package it.alessiosavi.cdk.initcluster;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

public class CdkInitClusterStack extends Stack {
  public CdkInitClusterStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public CdkInitClusterStack(final Construct scope, final String id,
                             final StackProps props) {
    super(scope, id, props);

    Table playerData = CreatePlayerDataTable("PlayerData");
    Function lambdaStats = CreateLambdaFunctionStats(playerData.getTableName());
    //        Function lambdaUpdater =
    //        CreateLambdaFunctionUpdater(playerData.getTableName());

    playerData.grantReadData(lambdaStats);

    RestApi api =
        RestApi.Builder.create(this, "OgameStats-REST-API")
            .restApiName("OgameStatsRestAPI")
            .description(
                "This service services return the possible candidates attack.")
            .build();

    LambdaIntegration ogameStatsIntegration =
        LambdaIntegration.Builder.create(lambdaStats)
            .requestTemplates(new HashMap<String, String>() {
              { put("application/json", "{ \"statusCode\": \"200\" }"); }
            })
            .build();

    api.getRoot().addMethod("POST", ogameStatsIntegration);
  }

  private Table CreatePlayerDataTable(String TABLE_NAME) {
    return Table.Builder.create(this, TABLE_NAME)
        .tableName(TABLE_NAME)
        .partitionKey(
            Attribute.builder().type(AttributeType.STRING).name("ID").build())
        .sortKey(Attribute.builder()
                     .type(AttributeType.STRING)
                     .name("Username")
                     .build())
        .removalPolicy(RemovalPolicy.RETAIN)
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

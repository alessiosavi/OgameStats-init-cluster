# OgameStats-init-cluster

 A little Java tool that rely on aws cdk in order to create a CloudFormation cluster
 
 
 
 ## Usage
 
 
Clone the necessary projects:
 ```bash
# Create the folder that will contains the project necessary for init the ogame stats
mkdir  -p /opt/SP/github/OgameStats
cd $_

git clone git@github.com:alessiosavi/OgameStats-init-cluster.git
git clone git@github.com:alessiosavi/GoStatOgame.git
git clone git@github.com:alessiosavi/PopulatePlayerDB.git

cd OgameStats-init-cluster.git
```

The first time, you need to create a stack on aws with the following command. This command have to be run only the first time.
```bash
cdk bootstrap
```


Now you can deploy see the cloudformation yaml file that will be generated:

```bash
cdk synth
```

At this point, you can deploy your stack with the following command:
```bash
cdk deploy
```

## Resources

- 1 DynamoDB table
- 2 Lambda function
  - One is scheduled every monday 12 A.M (
PopulatePlayerDB )
  - One related to the core functionality (GoStatOgame)
 - 2 ApiGateway RestApi for expose the Lambda
 - 1 Lambda EventInvokeConfig in order to schedule the lambda
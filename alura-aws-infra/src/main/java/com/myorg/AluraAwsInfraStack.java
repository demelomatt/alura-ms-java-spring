package com.myorg;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class AluraAwsInfraStack extends Stack {
    public AluraAwsInfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AluraAwsInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = createVpc();
        createDatabase(id, vpc);
        Cluster cluster = createCluster(vpc);
        createApplication(cluster);

    }

    public void createApplication(Cluster cluster) {
        Map<String, String> envs = new HashMap<>();

        envs.put("SPRING_DATASOURCE_URL", "jdbc:mysql://%s:3306/alurafood-pedidos?createDatabaseIfNotExists=true"
                        .formatted(Fn.importValue("pedidos-db-endpoint")));
        envs.put("SPRING_DATASOURCE_USERNAME", "admin");
        envs.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("pedidos-db-senha"));

        IRepository repository = Repository.fromRepositoryName(this, "my-repo", "img-pedidos-ms");
        ApplicationLoadBalancedFargateService build = ApplicationLoadBalancedFargateService.Builder.create(this, "AluraJavaMsFargate")
                .serviceName("alura-service-hello")
                .cluster(cluster)
                .cpu(512)
                .desiredCount(1)
                .listenerPort(8080)
                .assignPublicIp(true)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromEcrRepository(repository))
                                .containerPort(8080)
                                .containerName("app_hello")
                                .environment(envs)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "PedidosMsLogGroup")
                                                .logGroupName("PedidosMsLog")
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix("PedidosMS")
                                        .build()))
                                .build())
                .memoryLimitMiB(1024)
                .publicLoadBalancer(true)
                .build();

        ScalableTaskCount scalableTarget = build.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(1)
                .maxCapacity(3)
                .build());

        scalableTarget.scaleOnCpuUtilization("CpuScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(70)
                .scaleInCooldown(Duration.minutes(1))
                .scaleOutCooldown(Duration.minutes(2))
                .build());

        scalableTarget.scaleOnMemoryUtilization("MemoryScaling", MemoryUtilizationScalingProps.builder()
                .targetUtilizationPercent(65)
                .scaleInCooldown(Duration.minutes(1))
                .scaleOutCooldown(Duration.minutes(2))
                .build());

    }

    public Cluster createCluster(Vpc vpc) {
        return Cluster.Builder.create(this, "AluraJavaMsCluster")
                .vpc(vpc).build();
    }

    public Vpc createVpc() {
        return Vpc.Builder.create(this, "AluraJavaMsVpc")
                .maxAzs(3)
                .build();
    }

    public void createDatabase(String id, Vpc vpc) {
        CfnParameter pwd = createPwdParameter();

        ISecurityGroup iSecurityGroup = createSecurityGroup(id, vpc);

        DatabaseInstance database = DatabaseInstance.Builder
                .create(this, "Rds-pedidos")
                .instanceIdentifier("alura-aws-pedido-db")
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        .version(MysqlEngineVersion.VER_8_0)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername("admin",
                        CredentialsFromUsernameOptions.builder()
                                .password(SecretValue.unsafePlainText(pwd.getValueAsString()))
                                .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(iSecurityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();

        createOutputParameters(database, pwd);
    }

    private ISecurityGroup createSecurityGroup(String id, Vpc vpc) {
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));
        return iSecurityGroup;
    }

    private CfnParameter createPwdParameter() {
        return CfnParameter.Builder.create(this,"senha")
                .type("String")
                .description("Senha do database pedidos-ms")
                .build();
    }

    private void createOutputParameters(DatabaseInstance database, CfnParameter pwd) {
        CfnOutput.Builder.create(this, "pedidos-db-endpoint")
                .exportName("pedidos-db-endpoint")
                .value(database.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, "pedidos-db-senha")
                .exportName("pedidos-db-senha")
                .value(pwd.getValueAsString())
                .build();
    }
}

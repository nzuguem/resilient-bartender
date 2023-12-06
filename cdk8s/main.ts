import { Construct } from 'constructs';
import { App, Chart, ChartProps } from 'cdk8s';
import { IntOrString, KubeDeployment, KubeService } from './imports/k8s';

export class ResilientBartender extends Chart {
  constructor(scope: Construct, id: string, version: string, props: ChartProps = { }) {
    super(scope, id, props);

    new KubeService(this, 'svc', {
      spec: {
        type: 'NodePort',
        ports: [ { port: 8080, targetPort: IntOrString.fromString('http') } ],
        selector: {
           'app.kubernetes.io/component': 'api' , 
           'app.kubernetes.io/name': 'resilient-bartender'
          }
      }
    });

    new KubeDeployment(this, 'deploy', {
      metadata: {
        labels: {
          'app.kubernetes.io/version': version
        }
      },
      spec: {
        replicas: 3,
        strategy: {
          type: 'Recreate',
        },
        selector: {
          matchLabels: {
            'app.kubernetes.io/component': 'api' , 
            'app.kubernetes.io/name': 'resilient-bartender'
          }
        },
        template: {
          metadata: { 
            labels: {
              'app.kubernetes.io/component': 'api' , 
              'app.kubernetes.io/name': 'resilient-bartender',
              'app.kubernetes.io/version': version
            }
           },
          spec: {
            containers: [
              {
                name: 'resilient-bartender',
                image: `nzuguem/resilient-bartender:${version}`,
                ports: [ { containerPort: 8080 , name: 'http'} ],
                env: [
                  { 
                    name: 'INSTANCE_ID', 
                    valueFrom: {fieldRef : {fieldPath: 'metadata.name'}} }
                ]
              }
            ]
          }
        }
      }
    });

  }
}

const app = new App();
new ResilientBartender(app,'resilient-bartender', 'v1');
app.synth();

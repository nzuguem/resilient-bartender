import {ResilientBartender} from './main';
import {Testing} from 'cdk8s';

describe('Placeholder', () => {
  test('Empty', () => {
    const app = Testing.app();
    const chart = new ResilientBartender(app,'resilient-bartender', 'v0');
    const results = Testing.synth(chart)
    expect(results).toMatchSnapshot();
  });
});

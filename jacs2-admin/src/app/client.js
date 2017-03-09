import React from 'react';
import ReactDOM from 'react-dom';

// state and REST interface
import thunkMiddleware from 'redux-thunk'
import { Provider } from 'react-redux'
import { createStore, combineReducers, applyMiddleware } from 'redux'
import { fetchServices } from './actions'
import { rootReducer } from './reducers'
import { syncHistoryWithStore, routerReducer } from 'react-router-redux'

const store = createStore(
	combineReducers({
		rootReducer,
	    routing: routerReducer
	}),
    applyMiddleware(
       thunkMiddleware // lets us dispatch() functions
    )
)

// view
import { Router, IndexRoute, Route, Link, browserHistory } from 'react-router';
import App from 'grommet/components/App';
import Split from 'grommet/components/Split';
import Sidebar from 'grommet/components/Sidebar';
import Section from 'grommet/components/Section';
import 'grommet/scss/vanilla/index';

var Main = React.createClass({
    render: function() {
        return (
            <App centered={true}>
                <Split fixed={true}>
                    <Sidebar>
                        <ul>
                            <li><Link to="/Services">Services Queue</Link></li>
                            <li><Link to="/Pipes">Services Performance</Link></li>
                            <li><Link to="/servicesDetail">Services Detail</Link></li>
                            <li><Link to="/pipesQueue">Pipes Queue</Link></li>
                            <li><Link to="/pipesDetail">Pipes Detail</Link></li>
                            <li><Link to="/serviceRegistry">Service Registry</Link></li>
                            <li><Link to="/servicesAPI">Service API Detail</Link></li>
                        </ul>
                    </Sidebar>
                    <Section>
                        {this.props.children}
                    </Section>
                </Split>
            </App>
        );
    }
});

var Services = React.createClass({
    render: function() {
        return (
            <div>
               This is a services test
            </div>
        );
    }
});

var Pipes = React.createClass({
    render: function() {
        return (
            <div>
               This is a pipes test
            </div>
        );
    }
});

const history = syncHistoryWithStore(browserHistory, store)

ReactDOM.render(
<Provider store={store}>
    <Router history={history}>
        <Route path="/" component={Main}>
            <IndexRoute component={Services}/>
            <Route path="services" component={Services}/>
            <Route path="pipes" component={Pipes} />
        </Route>
    </Router>
</Provider>,
document.getElementById('app')

);

store.dispatch(fetchServices('localhost:8080')).then(() =>
  console.log(store.getState())
)
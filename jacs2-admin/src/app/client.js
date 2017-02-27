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
import { Layout, AppBar, NavDrawer, Navigation, Panel, Sidebar } from 'react-toolbox';
import theme from './App.scss';

var App = React.createClass({
    render: function() {
        return (
            <Layout theme={theme}>
                <NavDrawer
                    permanentAt='xl'
                    pinned>
                    <Navigation type='vertical'>
                        <ul>
                            <li><Link to="/servicesQueue">Services Queue</Link></li>
                            <li><Link to="/servicesPerformance">Services Performance</Link></li>
                            <li><Link to="/servicesDetail">Services Detail</Link></li>
                            <li><Link to="/pipesQueue">Pipes Queue</Link></li>
                            <li><Link to="/pipesDetail">Pipes Detail</Link></li>
                            <li><Link to="/serviceRegistry">Service Registry</Link></li>
                            <li><Link to="/servicesAPI">Service API Detail</Link></li>
                        </ul>
                    </Navigation>
                </NavDrawer>
                <Panel>
                    <AppBar leftIcon='menu' />
                     {this.props.children}
                </Panel>
            </Layout>
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
        <Route path="/" component={App}>
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
import React from 'react';
import ReactDOM from 'react-dom';

// state and REST interface
import thunkMiddleware from 'redux-thunk'
import { Provider } from 'react-redux'
import { createStore, combineReducers, applyMiddleware } from 'redux'
import { servicesRegistry } from './reducers'
import { syncHistoryWithStore, routerReducer } from 'react-router-redux'

const store = createStore(
	combineReducers({
		servicesRegistry,
	    routing: routerReducer
	}),
    {servicesRegistry: {services: []}},
    applyMiddleware(
       thunkMiddleware // lets us dispatch() functions
    )
)

// view
import { Router, IndexRoute, Route, Link, browserHistory } from 'react-router';
import App from 'grommet/components/App';
import Menu from 'grommet/components/Menu';
import Anchor from 'grommet/components/Anchor';
import Split from 'grommet/components/Split';
import Sidebar from 'grommet/components/Sidebar';
import Section from 'grommet/components/Section';

import { fetchServices } from './actions'
import ServiceTable from './ServiceTable';
import 'grommet/scss/vanilla/index';

var Main = React.createClass({
    render: function() {
        return (
            <App centered={true}>
                <Split flex={"right"}>
                    <Sidebar colorIndex="neutral-1">
                        <Menu fill={true} primary={true}>
                            <Anchor key="Services Queue" path="/services" label="Services Queue" />
                            <Anchor key="Services Performance" path="/services" label="Services Performance" />
                            <Anchor key="Services Detail" path="/services" label="Services Detail" />
                            <Anchor key="Pipes Queue" path="/pipes" label="Pipes Queue" />
                            <Anchor key="Pipes Detail" path="/pipes" label="Pipes Detail" />
                            <Anchor key="Service Registry" path="/pipes" label="Service Registry" />
                            <Anchor key="Service API Detail" path="/services" label="Service API Detail" />
                        </Menu>
                    </Sidebar>
                    <Section>
                        {this.props.children}
                    </Section>
                </Split>
            </App>
        );
    }
});

const history = syncHistoryWithStore(browserHistory, store)

ReactDOM.render(
<Provider store={store}>
    <Router history={history}>
        <Route path="/" component={Main}>
            <IndexRoute component={ServiceTable}/>
            <Route path="services" component={ServiceTable}/>
            <Route path="pipes" component={ServiceTable} />
        </Route>
    </Router>
</Provider>,
document.getElementById('app')

);

import React from 'react';
import { AppBar } from 'react-toolbox';
import { Layout, NavDrawer, Navigation, Panel, Sidebar } from 'react-toolbox';
import { Router, Route, Link } from 'react-router';
import theme from './App.scss';

var App = React.createClass({
    render: function() {
        return (
            <Layout theme={theme}>
                <NavDrawer
                    permanentAt='xxxl'
                    pinned>
                    <Navigation type='vertical'>
                        <Link to="/services">Services</Link>
                        <Link to="/pipes">Pipes</Link>
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


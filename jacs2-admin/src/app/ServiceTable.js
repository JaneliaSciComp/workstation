import React, { Component } from 'react';
import { connect } from 'react-redux';
import Table from 'grommet/components/Table';
import TableHeader from 'grommet/components/TableHeader';
import ServiceRowItem from './ServiceRowItem';
import { fetchServices } from './actions'

class ServiceTable extends Component {
  componentDidMount() {
      this.props.fetchServices('localhost:8080');
  }

  render() {
      return (<Table full={true} scrollable={false} selectable={true}>
          <TableHeader labels={['Name', 'Description']} />
          <tbody>
               {this.props.services.map((service) =>
                  <ServiceRowItem key={service.serviceName} service={service}/>
              )} 
          </tbody>
      </Table>);
  }
}

const mapStateToProps = (state) => {
    return {
        services: state.servicesRegistry.services
    };
};
const mapDispatchToProps = (dispatch) => {
    return {
        fetchServices: (url) => dispatch(fetchServices(url))
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(ServiceTable);
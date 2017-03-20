import React from 'react';
import ReactDOM from 'react-dom';
import TableRow from 'grommet/components/TableRow';

const ServiceRowItem = ({ service }) => (
    <TableRow>
      <td>
        {service.serviceName}
      </td>
      <td>
        {service.usage}
      </td>
    </TableRow>
)

export default ServiceRowItem
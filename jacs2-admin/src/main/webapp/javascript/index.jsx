import ReactDOM from 'react-dom';
import React from 'react';
import Table from './table';
import { Button } from 'react-toolbox/lib/button';
import '../css/general.css';

ReactDOM.render(
  <div>
    <Table number={1} openSeats={[1,2]}/>
    <Table number={2} openSeats={[1,2,3]}/>
    <Table number={3} openSeats={[1]}/>
    <Table number={4} openSeats={[1,2,3,4]}/>
<Button label="Hello World!" />
  </div>,
  document.getElementById('content')
);

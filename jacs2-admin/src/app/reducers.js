import { combineReducers } from 'redux'
import {
  RELOAD_SERVICES_QUEUE, RECEIVE_SERVICES_QUEUE
} from './actions'

function servicesQueue(state = {
  didInvalidate: false,
  services: []
}, action) {
  switch (action.type) {
    case RELOAD_SERVICES_QUEUE:
      return Object.assign({}, state, {
        didInvalidate: true
      })
    case RECEIVE_SERVICES_QUEUE:
      return Object.assign({}, state, {
        didInvalidate: false,
        services: action.services,
        lastUpdated: action.receivedAt
      })
    default:
      return state
  }
}


export const rootReducer = combineReducers({
  servicesQueue
});

export default rootReducer
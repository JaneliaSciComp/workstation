import { combineReducers } from 'redux'
import {
  RELOAD_SERVICES_QUEUE, RECEIVE_SERVICES_QUEUE
} from './actions'

export function servicesRegistry(state = {
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

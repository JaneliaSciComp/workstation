export const RELOAD_SERVICES_QUEUE = 'RELOAD_SERVICES'

export function reloadServices(url) {
  return {
    type: RELOAD_SERVICES_QUEUE,
  }
}

export const RECEIVE_SERVICES_QUEUE = 'RECEIVE_SERVICES_QUEUE'

export function receiveServices(url, json) {
  return {
    type: RECEIVE_SERVICES_QUEUE,
    services: json,
    receivedAt: Date.now()
  }
}

export function fetchServices(url) {
  var endpoint = 'http://' + url + '/jacs/jacs-api/v2/services/metadata';
  return dispatch => {
    dispatch(reloadServices(url))
    return fetch(endpoint)
      .then(response => response.json())
      .then(json => dispatch(receiveServices(url, json)))
  }
}
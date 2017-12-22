import './main.css';
import { Main } from './Main.elm';
import registerServiceWorker from './registerServiceWorker';

var app = Main.embed(document.getElementById('root'));

registerServiceWorker();

app.ports.renderGoogleSSOButton.subscribe(function(id) {
    renderGoogleSSOButton(id);
});

function onSuccess(googleUser) {
    console.log('Logged in as: ' + googleUser.getBasicProfile().getName());
    app.ports.username.send(googleUser.getBasicProfile().getEmail());
}
function onFailure(error) {
    console.log(error);
}

function renderGoogleSSOButton(id) {
    gapi.signin2.render(id, {
        'scope': 'profile email',
        'width': 240,
        'height': 50,
        'longtitle': true,
        'theme': 'dark',
        'onsuccess': onSuccess,
        'onfailure': onFailure
    });
}

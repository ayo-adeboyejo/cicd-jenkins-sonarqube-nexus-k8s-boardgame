'use strict';

// -------------------------------------------------------
// Constants
// -------------------------------------------------------
const ROLE_USER    = 'ROLE_USER';
const ROLE_MANAGER = 'ROLE_MANAGER';
const ERROR_EMPTY_FIELDS  = 'User name and password are required';
const ERROR_NO_ROLE       = 'You must select at least one role';
const ERROR_MANAGER_ROLE  = 'To add a manager role, you should ALSO check a user role';

// -------------------------------------------------------
// Validates the new user form before submission
// -------------------------------------------------------
function verify() {
    clearError();

    const password = document.forms['form']['password'].value;
    const userName = document.forms['form']['userName'].value;

    if (!password || !userName) {
        showError(ERROR_EMPTY_FIELDS);
        return false;
    }

    const selectedRoles = getSelectedRoles();

    if (selectedRoles.length === 0) {
        showError(ERROR_NO_ROLE);
        return false;
    }

    if (selectedRoles.includes(ROLE_MANAGER) && !selectedRoles.includes(ROLE_USER)) {
        showError(ERROR_MANAGER_ROLE);
        return false;
    }

    return true;
}

// -------------------------------------------------------
// Returns array of checked role values
// -------------------------------------------------------
function getSelectedRoles() {
    const checkboxes = document.getElementsByName('authorities');
    const selected = [];

    for (let i = 0; i < checkboxes.length; i++) {
        if (checkboxes[i].checked) {
            selected.push(checkboxes[i].value);
        }
    }

    return selected;
}

// -------------------------------------------------------
// Displays an error message in the error element
// -------------------------------------------------------
function showError(message) {
    const errorEl = document.getElementById('error');
    if (errorEl) {
        errorEl.innerHTML = message;
    }
}

// -------------------------------------------------------
// Clears any existing error message
// -------------------------------------------------------
function clearError() {
    showError('');
}

// -------------------------------------------------------
// When ROLE_MANAGER is toggled, automatically
// check or uncheck ROLE_USER accordingly
// -------------------------------------------------------
document.addEventListener('DOMContentLoaded', function () {
    const managerCheckbox = document.getElementById(ROLE_MANAGER);
    const userCheckbox    = document.getElementById(ROLE_USER);

    if (managerCheckbox && userCheckbox) {
        managerCheckbox.addEventListener('click', function () {
            userCheckbox.checked = managerCheckbox.checked;
        });
    }
});

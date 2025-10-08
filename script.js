
//SELECT ALL ELEMENTS
// --- PAGE SECTIONS ---
const loginPage = document.getElementById('login-page');
const signupPage = document.getElementById('signup-page');
const homePage = document.getElementById('home-page');
const meetingPage = document.getElementById('meeting-page');
const emailPage = document.getElementById('email-page');
// --- LOGIN PAGE ELEMENTS ---
const loginUsername = document.getElementById('login-username');
const loginPassword = document.getElementById('login-password');
const loginBtn = document.getElementById('login-btn');
const loginError = document.getElementById('login-error');
const loginLoading = document.getElementById('login-loading');
const gotoSignup = document.getElementById('goto-signup');
// --- SIGNUP PAGE ELEMENTS ---
const signupUsername = document.getElementById('signup-username');
const signupEmail = document.getElementById('signup-email');
const signupPassword = document.getElementById('signup-password');
const signupConfirmPassword = document.getElementById('signup-confirm-password');
const signupBtn = document.getElementById('signup-btn');
const signupError = document.getElementById('signup-error');
const signupSuccess = document.getElementById('signup-success');
const signupLoading = document.getElementById('signup-loading');
const gotoLogin = document.getElementById('goto-login');
// --- HOME PAGE ELEMENTS ---
const logoutBtnHome = document.getElementById('logout-btn-home');
const gotoMeetingBtn = document.getElementById('goto-meeting-btn');
const gotoEmailBtn = document.getElementById('goto-email-btn');
const welcomeMessage = document.getElementById('welcome-message');
// --- MEETING PAGE ELEMENTS ---
const logoutBtnMeeting = document.getElementById('logout-btn-meeting');
const backToHomeMeeting = document.getElementById('back-to-home-meeting');
const meetingPeople = document.getElementById('meeting-people');
const meetingLocation = document.getElementById('meeting-location');
const meetingTimeBegin = document.getElementById('meeting-time-begin');
const meetingTimeEnd = document.getElementById('meeting-time-end');
const meetingDate = document.getElementById('meeting-date');
const meetingSubject = document.getElementById('meeting-subject');
const meetingDetails = document.getElementById('meeting-details');
const meetingSubmitBtn = document.getElementById('meeting-submit-btn');
const meetingLoading = document.getElementById('meeting-loading');
const meetingResult = document.getElementById('meeting-result');
// --- EMAIL PAGE ELEMENTS ---
const logoutBtnEmail = document.getElementById('logout-btn-email');
const backToHomeEmail = document.getElementById('back-to-home-email');
const emailRecipients = document.getElementById('email-recipients');
const emailContent = document.getElementById('email-content');
const emailSubmitBtn = document.getElementById('email-submit-btn');
const emailLoading = document.getElementById('email-loading');
const emailSuccess = document.getElementById('email-success');
const emailError = document.getElementById('email-error');
// ========================================
// STEP 2: GLOBAL VARIABLESp
// This will store the logged-in user's information
let currentUser = null;
// Backend API base URL
const API_BASE_URL = 'http://localhost:3000';
// ========================================
// STEP 3: UTILITY FUNCTIONS
/*
 * Hides all pages - used before showing a specific page
 */
function hideAllPages() {
    loginPage.classList.add('hidden');
    signupPage.classList.add('hidden');
    homePage.classList.add('hidden');
    meetingPage.classList.add('hidden');
    emailPage.classList.add('hidden');
}
/**
 * Shows a specific page with scale + fade animation
 * @param {HTMLElement} page - The page element to show
 */
function showPage(page) {
    // Hide all pages (they'll fade out via CSS transition)
    hideAllPages();

    // Show new page (it'll fade in via animation)
    page.classList.remove('hidden');
    page.classList.add('scale-fade-in');

    // Clean up
    setTimeout(() => {
        page.classList.remove('scale-fade-in');
    }, 400);
}
/**
 * Displays an error message in a specific element
 * @param {HTMLElement} element - The error message container
 * @param {string} message - The error text to display
 */
function showError(element, message) {
    element.textContent = message; // Set the text
    element.classList.remove('hidden'); // Make it visible
}

/**
 * Hides an error message
 * @param {HTMLElement} element - The error message container
 */
function hideError(element) {
    element.classList.add('hidden'); // Hide it
    element.textContent = ''; // Clear the text
}

/**
 * Validates email format
 */
function isValidEmail(email) {
    // checking for: something@something.something
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email); // Returns true or false
}

/**
 * Validates that a string contains only letters, spaces, and common punctuation
 * Used for names, subjects, etc.
 */
function isValidText(text) {
    // This pattern allows letters (any language), spaces, commas, periods, hyphens
    const textRegex = /^[a-zA-ZÀ-ÿ\s,.\-']+$/;
    return textRegex.test(text);
}

/**
 * Clears all input fields on a page (used after successful submission)
 */
function clearInputs(inputs) {
    inputs.forEach(input => {
        input.value = '';
    });
}
// ========================================
//PAGE NAVIGATION
/**
 * Navigate to the signup page
 */
gotoSignup.addEventListener('click', function(e) {
    e.preventDefault(); // Prevents the default link behavior (no page reload)
    showPage(signupPage); // Show signup page
    hideError(loginError); // Clear any login errors
});
/**
 * Navigate back to the login page from signup
 */
gotoLogin.addEventListener('click', function(e) {
    e.preventDefault();
    showPage(loginPage); // Show login page
    hideError(signupError); // Clear any signup errors
    hideError(signupSuccess); // Clear success message
});
/**
 * Navigate to meeting page from home
 */
gotoMeetingBtn.addEventListener('click', function() {
    showPage(meetingPage);
});
/**
 * Navigate to email page from home
 */
gotoEmailBtn.addEventListener('click', function() {
    showPage(emailPage);
});
/**
 * Navigate back to home page from meeting page
 */
backToHomeMeeting.addEventListener('click', function() {
    showPage(homePage);
    // Clear any meeting form data/messages
    hideError(meetingResult);
    meetingResult.classList.remove('valid', 'invalid');
});
/**
 * Navigate back to home page from email page
 */
backToHomeEmail.addEventListener('click', function() {
    showPage(homePage);
    // Clear any email messages
    hideError(emailSuccess);
    hideError(emailError);
});
/**
 * Logout function - clears user data and returns to login
 * This is used by all logout buttons
 */
function logout() {
    currentUser = null; // Clear user data
    showPage(loginPage); // Go back to login page
    // Clear all form inputs
    clearInputs([loginUsername, loginPassword]);
}
// Add logout functionality to all logout buttons
logoutBtnHome.addEventListener('click', logout);
logoutBtnMeeting.addEventListener('click', logout);
logoutBtnEmail.addEventListener('click', logout);
// ========================================
// STEP 5: LOGIN FUNCTIONALITY
loginBtn.addEventListener('click', async function() {
    // Get the values the user typed
    const username = loginUsername.value.trim();
    const password = loginPassword.value.trim();
    // VALIDATION: Check if fields are empty
    if (!username || !password) {
        showError(loginError, 'Please fill in all fields');
        return;
    }
    hideError(loginError);
    loginLoading.classList.remove('hidden');
    loginBtn.disabled = true; // Disable button to prevent multiple clicks
    try {
        const response = await fetch(`${API_BASE_URL}/api/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ // Convert JavaScript object to JSON string
                username: username,
                password: password
            })
        });
        const data = await response.json();
        // Check if login was successful
        if (data.status === 'success') {
            currentUser = data.user; // Store user data globally
            if (data.user && data.user.name) {
                welcomeMessage.textContent = `Hello ${data.user.name}, what can I assist you with?`;
            }
            // Clear form and go to home page
            clearInputs([loginUsername, loginPassword]);
            showPage(homePage);
        } else {
            // FAILURE: Show error message from backend
            showError(loginError, data.message || 'Login failed. Please try again.');
        }
    } catch (error) {
        // NETWORK ERROR: Something went wrong with the request
        console.error('Login error:', error);
        showError(loginError, 'Network error. Please check your connection.');
    } finally {
        loginLoading.classList.add('hidden');
        loginBtn.disabled = false;
    }
});
// ========================================
// STEP 6: SIGNUP FUNCTIONALITY
signupBtn.addEventListener('click', async function() {
    const username = signupUsername.value.trim();
    const email = signupEmail.value.trim();
    const password = signupPassword.value.trim();
    const confirmPassword = signupConfirmPassword.value.trim();
    // VALIDATION: Check if any field is empty
    if (!username || !email || !password || !confirmPassword) {
        showError(signupError, 'Please fill in all fields');
        return;
    }
    if (username.length < 3) {
        showError(signupError, 'Username must be at least 3 characters');
        return;
    }
    if (!isValidEmail(email)) {
        showError(signupError, 'Please enter a valid email address');
        return;
    }
    if (password.length < 8) {
        showError(signupError, 'Password must be at least 8 characters');
        return;
    }
    if (password !== confirmPassword) {
        showError(signupError, 'Passwords do not match');
        return;
    }
    // All validations passed - clear errors and show loading
    hideError(signupError);
    signupLoading.classList.remove('hidden');
    signupBtn.disabled = true;
    try {
        const response = await fetch(`${API_BASE_URL}/api/signup`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                email: email,
                password: password
            })
        });
        const data = await response.json();
        if (data.status === 'success') {
            signupSuccess.textContent = 'Account created successfully! Redirecting to login...';
            signupSuccess.classList.remove('hidden');
            clearInputs([signupUsername, signupEmail, signupPassword, signupConfirmPassword]);
            setTimeout(() => {
                showPage(loginPage);
                hideError(signupSuccess);
            }, 2000); // 2000 milliseconds = 2 seconds
        } else if (data.status === 'exist'){
            showError(signupError, data.message || 'You already have an account.');
        } 
        else {
        showError(signupError, data.message || 'Signup failed. Please try again.');
        }
    } catch (error) {
        // NETWORK ERROR
        console.error('Signup error:', error);
        showError(signupError, 'Network error. Please check your connection.');
    } finally {
        // CLEANUP
        signupLoading.classList.add('hidden');
        signupBtn.disabled = false;
    }
});
// ========================================
// STEP 7: MEETING FORM FUNCTIONALITY
meetingSubmitBtn.addEventListener('click', async function() {
    const people = meetingPeople.value.trim();
    const location = meetingLocation.value.trim();
    const timeBegin = meetingTimeBegin.value;
    const timeEnd = meetingTimeEnd.value;
    const date = meetingDate.value.trim();
    const subject = meetingSubject.value.trim();
    const details = meetingDetails.value.trim();
    if (!people || !location || !timeBegin || !timeEnd || !date || !subject || !details) {
        meetingResult.textContent = 'Please fill in all fields';
        meetingResult.className = 'invalid'; // Add 'invalid' class for red styling
        meetingResult.classList.remove('hidden');
        return;
    }
    if (!isValidText(people)) {
        meetingResult.textContent = 'People names should contain only letters';
        meetingResult.className = 'invalid';
        meetingResult.classList.remove('hidden');
        return;
    }
    if (!isValidText(location)) {
        meetingResult.textContent = 'Location should contain only letters';
        meetingResult.className = 'invalid';
        meetingResult.classList.remove('hidden');
        return;
    }
    // VALIDATION: Check if end time is after begin time
    if (timeEnd < timeBegin) {
        meetingResult.textContent = 'End time must be after start time';
        meetingResult.className = 'invalid';
        meetingResult.classList.remove('hidden');
        return;
    }
    // All validations passed - hide any previous results and show loading
    meetingResult.classList.add('hidden');
    meetingLoading.classList.remove('hidden');
    meetingSubmitBtn.disabled = true;
    try {
        const response = await fetch(`${API_BASE_URL}/api/meeting/validate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                people: people,
                location: location,
                timeBegin: timeBegin,
                timeEnd: timeEnd,
                date: date,
                subject: subject,
                details: details
            })
        });
        const data = await response.json();
        // Show result based on backend response
        meetingResult.classList.remove('hidden');
        if (data.status === 'valid') {
            // VALID: Show success message
            meetingResult.textContent = data.message || 'Meeting is valid!';
            meetingResult.className = 'valid'; // Green styling
        } else {
            // INVALID: Show error message
            meetingResult.textContent = data.message || 'Meeting validation failed';
            meetingResult.className = 'invalid'; // Red styling
        }
    } catch (error) {
        // NETWORK ERROR
        console.error('Meeting validation error:', error);
        meetingResult.textContent = 'Network error. Please try again.';
        meetingResult.className = 'invalid';
        meetingResult.classList.remove('hidden');
    } finally {
        // CLEANUP
        meetingLoading.classList.add('hidden');
        meetingSubmitBtn.disabled = false;
    }
});
// ========================================
// STEP 8: EMAIL FORM FUNCTIONALITY
emailSubmitBtn.addEventListener('click', async function() {
    const recipients = emailRecipients.value.trim();
    const content = emailContent.value.trim();
    if (!recipients || !content) {
        showError(emailError, 'Please fill in all fields');
        hideError(emailSuccess);
        return;
    }
    // VALIDATION: Check email format(s)
    // Split by comma in case of multiple emails
    const emailList = recipients.split(',').map(email => email.trim());
    // Check each email
    for (let email of emailList) {
        if (!isValidEmail(email)) {
            showError(emailError, `Invalid email format: ${email}`);
            hideError(emailSuccess);
            return;
        }
    }
    hideError(emailError);
    hideError(emailSuccess);
    emailLoading.classList.remove('hidden');
    emailSubmitBtn.disabled = true;
    try {
        // BACKEND CALL: Send email data
        const response = await fetch(`${API_BASE_URL}/api/email/send`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                recipients: emailList,
                content: content
            })
        });
        const data = await response.json();
        if (data.status === 'success') {
            emailSuccess.textContent = data.message || 'Email sent successfully!';
            emailSuccess.classList.remove('hidden');
            // Clear the form
            clearInputs([emailRecipients, emailContent]);
        } else {
            showError(emailError, data.message || 'Failed to send email');
        }
    } catch (error) {
        // NETWORK ERROR
        console.error('Email send error:', error);
        showError(emailError, 'Network error. Please try again.');
    } finally {
        // CLEANUP
        emailLoading.classList.add('hidden');
        emailSubmitBtn.disabled = false;
    }
});


//INITIALIZATION: backstage
// This code runs when the page first loads
showPage(loginPage);
console.log('Website JavaScript loaded successfully!');

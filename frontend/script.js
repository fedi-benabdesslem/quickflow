const API_BASE_URL = 'http://localhost:8080';

const loginPage = document.getElementById('login-page');
const signupPage = document.getElementById('signup-page');
const homePage = document.getElementById('home-page');
const meetingPage = document.getElementById('meeting-page');
const emailPage = document.getElementById('email-page');
const reviewPage = document.getElementById('review-page');

const loginUsername = document.getElementById('login-username');
const loginPassword = document.getElementById('login-password');
const loginBtn = document.getElementById('login-btn');
const loginError = document.getElementById('login-error');
const loginLoading = document.getElementById('login-loading');
const gotoSignup = document.getElementById('goto-signup');

const signupUsername = document.getElementById('signup-username');
const signupEmail = document.getElementById('signup-email');
const signupPassword = document.getElementById('signup-password');
const signupConfirmPassword = document.getElementById('signup-confirm-password');
const signupBtn = document.getElementById('signup-btn');
const signupError = document.getElementById('signup-error');
const signupSuccess = document.getElementById('signup-success');
const signupLoading = document.getElementById('signup-loading');
const gotoLogin = document.getElementById('goto-login');

const logoutBtnHome = document.getElementById('logout-btn-home');
const gotoMeetingBtn = document.getElementById('goto-meeting-btn');
const gotoEmailBtn = document.getElementById('goto-email-btn');
const welcomeMessage = document.getElementById('welcome-message');

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
const meetingError = document.getElementById('meeting-error');
const meetingResult = document.getElementById('meeting-result');

const logoutBtnEmail = document.getElementById('logout-btn-email');
const backToHomeEmail = document.getElementById('back-to-home-email');
const emailRecipients = document.getElementById('email-recipients');
const emailContent = document.getElementById('email-content');
const emailSubmitBtn = document.getElementById('email-submit-btn');
const emailLoading = document.getElementById('email-loading');
const emailSuccess = document.getElementById('email-success');
const emailError = document.getElementById('email-error');

const reviewType = document.getElementById('review-type');
const reviewSubject = document.getElementById('review-subject');
const reviewRecipients = document.getElementById('review-recipients');
const reviewPeople = document.getElementById('review-people');
const reviewLocation = document.getElementById('review-location');
const reviewDate = document.getElementById('review-date');
const reviewContent = document.getElementById('review-content');
const reviewEmailFields = document.getElementById('review-email-fields');
const reviewPvFields = document.getElementById('review-pv-fields');
const reviewSendBtn = document.getElementById('review-send-btn');
const reviewEditBtn = document.getElementById('review-edit-btn');
const reviewLoading = document.getElementById('review-loading');
const reviewSuccess = document.getElementById('review-success');
const reviewError = document.getElementById('review-error');
const backToHomeReview = document.getElementById('back-to-home-review');
const logoutBtnReview = document.getElementById('logout-btn-review');

let currentUser = null;
let currentReviewType = '';
let currentGeneratedId = '';

function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function showError(element, message) {
    if (element) {
        element.textContent = message;
        element.classList.remove('hidden');
    }
}

function hideError(element) {
    if (element) {
        element.classList.add('hidden');
        element.textContent = '';
    }
}

function clearInputs(inputs) {
    inputs.forEach(input => {
        if (input) input.value = '';
    });
}

function showPage(page) {
    if (!page) return;
    document.querySelectorAll('.page').forEach(p => {
        if (p) p.classList.add('hidden');
    });
    page.classList.remove('hidden');
}

function toggleLoading(loadingElement, show) {
    if (loadingElement) {
        loadingElement.classList.toggle('hidden', !show);
    }
}

function toggleButton(button, disabled) {
    if (button) button.disabled = disabled;
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    currentUser = null;
    showPage(loginPage);
}

function safeAddEventListener(element, event, handler) {
    if (element && handler) {
        element.addEventListener(event, handler);
    }
}

safeAddEventListener(gotoSignup, 'click', function(e) {
    e.preventDefault();
    showPage(signupPage);
});

safeAddEventListener(gotoLogin, 'click', function(e) {
    e.preventDefault();
    showPage(loginPage);
});

safeAddEventListener(backToHomeMeeting, 'click', function() {
    showPage(homePage);
});

safeAddEventListener(backToHomeEmail, 'click', function() {
    showPage(homePage);
});

safeAddEventListener(backToHomeReview, 'click', function() {
    showPage(homePage);
    clearReviewForm();
});

safeAddEventListener(gotoMeetingBtn, 'click', function() {
    showPage(meetingPage);
});

safeAddEventListener(gotoEmailBtn, 'click', function() {
    showPage(emailPage);
});

safeAddEventListener(logoutBtnHome, 'click', logout);
safeAddEventListener(logoutBtnMeeting, 'click', logout);
safeAddEventListener(logoutBtnEmail, 'click', logout);
safeAddEventListener(logoutBtnReview, 'click', logout);

safeAddEventListener(loginBtn, 'click', async function() {
    const username = loginUsername ? loginUsername.value.trim() : '';
    const password = loginPassword ? loginPassword.value.trim() : '';
    
    if (!username || !password) {
        showError(loginError, 'Username and password required');
        return;
    }

    hideError(loginError);
    toggleLoading(loginLoading, true);
    toggleButton(loginBtn, true);

    try {
        const response = await fetch(`${API_BASE_URL}/api/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const data = await response.json();
        
        if (data.status === 'success') {
            localStorage.setItem('token', data.token);
            localStorage.setItem('userId', data.user.id);
            localStorage.setItem('username', data.user.username);
            currentUser = data.user;
            
            if (welcomeMessage) welcomeMessage.textContent = data.user.username;
            showPage(homePage);
            clearInputs([loginUsername, loginPassword]);
        } else {
            showError(loginError, data.message || 'Login failed');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError(loginError, 'Network error. Check connection.');
    } finally {
        toggleLoading(loginLoading, false);
        toggleButton(loginBtn, false);
    }
});

safeAddEventListener(signupBtn, 'click', async function() {
    const username = signupUsername ? signupUsername.value.trim() : '';
    const email = signupEmail ? signupEmail.value.trim() : '';
    const password = signupPassword ? signupPassword.value.trim() : '';
    const confirmPassword = signupConfirmPassword ? signupConfirmPassword.value.trim() : '';

    if (!username || !email || !password || !confirmPassword) {
        showError(signupError, 'All fields required');
        return;
    }
    if (password !== confirmPassword) {
        showError(signupError, 'Passwords do not match');
        return;
    }
    if (!isValidEmail(email)) {
        showError(signupError, 'Invalid email format');
        return;
    }

    hideError(signupError);
    hideError(signupSuccess);
    toggleLoading(signupLoading, true);
    toggleButton(signupBtn, true);

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
            if (signupSuccess) {
                signupSuccess.textContent = data.message || 'Account created!';
                signupSuccess.classList.remove('hidden');
            }
            setTimeout(() => {
                showPage(loginPage);
                clearInputs([signupUsername, signupEmail, signupPassword, signupConfirmPassword]);
            }, 2000);
        } else {
            showError(signupError, data.message || 'Signup failed');
        }
    } catch (error) {
        console.error('Signup error:', error);
        showError(signupError, 'Network error. Try again.');
    } finally {
        toggleLoading(signupLoading, false);
        toggleButton(signupBtn, false);
    }
});

safeAddEventListener(meetingSubmitBtn, 'click', async function() {
    const people = meetingPeople ? meetingPeople.value.trim() : '';
    const location = meetingLocation ? meetingLocation.value.trim() : '';
    const timeBegin = meetingTimeBegin ? meetingTimeBegin.value : '';
    const timeEnd = meetingTimeEnd ? meetingTimeEnd.value : '';
    const date = meetingDate ? meetingDate.value : '';
    const subject = meetingSubject ? meetingSubject.value.trim() : '';
    const details = meetingDetails ? meetingDetails.value.trim() : '';

    if (!people || !location || !timeBegin || !timeEnd || !date || !subject) {
        showError(meetingError, 'Please fill in all fields');
        return;
    }
    if (new Date(timeEnd) <= new Date(timeBegin)) {
        showError(meetingError, 'End time must be after start time');
        return;
    }

    hideError(meetingError);
    toggleLoading(meetingLoading, true);
    toggleButton(meetingSubmitBtn, true);

    const formData = {
        people: people.split(',').map(p => p.trim()),
        location: location,
        timeBegin: timeBegin,
        timeEnd: timeEnd,
        date: date,
        subject: subject,
        details: details
    };

    try {
        const token = localStorage.getItem('token');
        if (!token) {
            showError(meetingError, 'Authentication required. Please log in.');
            return;
        }

        const response = await fetch(`${API_BASE_URL}/api/meeting/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(formData)
        });

        const data = await response.json();
        if (data.status === 'success') {
            currentReviewType = 'pv';
            currentGeneratedId = data.id || data.meetingId;
            if (reviewType) reviewType.textContent = 'Review Meeting Summary (PV)';
            if (reviewPvFields) reviewPvFields.classList.remove('hidden');
            if (reviewEmailFields) reviewEmailFields.classList.add('hidden');
            if (reviewSubject) reviewSubject.value = data.subject || subject;
            if (reviewContent) reviewContent.value = data.generatedContent;
            if (reviewPeople) reviewPeople.value = people;
            if (reviewLocation) reviewLocation.value = location;
            if (reviewDate) reviewDate.value = date;
            showPage(reviewPage);
            clearInputs([meetingPeople, meetingLocation, meetingTimeBegin, meetingTimeEnd, meetingDate, meetingSubject, meetingDetails]);
        } else {
            showError(meetingError, data.message || 'Failed to generate summary');
        }
    } catch (error) {
        console.error('Meeting error:', error);
        showError(meetingError, 'Network error. Try again.');
    } finally {
        toggleLoading(meetingLoading, false);
        toggleButton(meetingSubmitBtn, false);
    }
});

safeAddEventListener(emailSubmitBtn, 'click', async function() {
    const recipients = emailRecipients ? emailRecipients.value.trim() : '';
    const content = emailContent ? emailContent.value.trim() : '';
    if (!recipients || !content) {
        showError(emailError, 'Please fill in all fields');
        hideError(emailSuccess);
        return;
    }

    const emailList = recipients.split(',').map(email => email.trim());
    for (let email of emailList) {
        if (!isValidEmail(email)) {
            showError(emailError, `Invalid email: ${email}`);
            hideError(emailSuccess);
            return;
        }
    }

    hideError(emailError);
    hideError(emailSuccess);
    toggleLoading(emailLoading, true);
    toggleButton(emailSubmitBtn, true);

    const userId = localStorage.getItem('userId') || 'anonymous';
    const token = localStorage.getItem('token');
    if (!token) {
        showError(emailError, 'Authentication required. Please log in.');
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/api/email/send`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                recipients: emailList,
                userId: userId,
                input: content,
                bulletPoints: []
            })
        });

        const data = await response.json();
        if (data.status === 'success') {
            if (emailSuccess) {
                emailSuccess.textContent = 'Email generated! Redirecting to review...';
                emailSuccess.classList.remove('hidden');
            }
            setTimeout(() => {
                currentReviewType = 'email';
                currentGeneratedId = data.emailId;
                if (reviewType) reviewType.textContent = 'Review Email Draft';
                if (reviewEmailFields) reviewEmailFields.classList.remove('hidden');
                if (reviewPvFields) reviewPvFields.classList.add('hidden');
                if (reviewSubject) reviewSubject.value = data.subject || 'Draft Email';
                if (reviewContent) reviewContent.value = data.generatedContent;
                if (reviewRecipients) reviewRecipients.value = recipients;    
                showPage(reviewPage);
                clearInputs([emailRecipients, emailContent]);
            }, 1500);
        } else {
            showError(emailError, data.message || 'Failed to generate email');
        }
    } catch (error) {
        console.error('Email error:', error);
        showError(emailError, 'Network error. Try again.');
    } finally {
        toggleLoading(emailLoading, false);
        toggleButton(emailSubmitBtn, false);
    }
});

safeAddEventListener(backToHomeReview, 'click', function() {
    showPage(homePage);
    clearReviewForm();
});

safeAddEventListener(logoutBtnReview, 'click', logout);

safeAddEventListener(reviewSendBtn, 'click', async function() {
    const subject = reviewSubject ? reviewSubject.value.trim() : '';
    const content = reviewContent ? reviewContent.value.trim() : '';
    if (!content || !subject) {
        showError(reviewError, 'Subject and content required');
        return;
    }

    toggleLoading(reviewLoading, true);
    toggleButton(reviewSendBtn, true);
    const token = localStorage.getItem('token');
    if (!token) {
        showError(reviewError, 'Authentication required. Please log in.');
        return;
    }
    try {
        const response = await fetch(`${API_BASE_URL}/api/${currentReviewType}/send-final`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                id: currentGeneratedId,
                subject: subject,
                content: content,
                ...(currentReviewType === 'email' && { recipients: (reviewRecipients ? reviewRecipients.value.split(',').map(r => r.trim()) : []) }),
                ...(currentReviewType === 'pv' && { 
                    people: (reviewPeople ? reviewPeople.value.split(',').map(p => p.trim()) : []),
                    location: reviewLocation ? reviewLocation.value : '',
                    date: reviewDate ? reviewDate.value : ''
                })
            })
        });
        const data = await response.json();
        if (data.status === 'success') {
            if (reviewSuccess) {
                reviewSuccess.textContent = data.message || 'Sent successfully!';
                reviewSuccess.classList.remove('hidden');
            }
            setTimeout(() => {
                showPage(homePage);
            }, 2000);
        } else {
            showError(reviewError, data.message || 'Failed to send');
        }
    } catch (error) {
        console.error('Send error:', error);
        showError(reviewError, 'Network error. Try again.');
    } finally {
        toggleLoading(reviewLoading, false);
        toggleButton(reviewSendBtn, false);
    }
});

safeAddEventListener(reviewEditBtn, 'click', function() {
    if (currentReviewType === 'email') {
        showPage(emailPage);
    } else if (currentReviewType === 'pv') {
        showPage(meetingPage);
    }
    clearReviewForm();
});

function clearReviewForm() {
    if (reviewSubject) reviewSubject.value = '';
    if (reviewContent) reviewContent.value = '';
    if (reviewRecipients) reviewRecipients.value = '';
    if (reviewPeople) reviewPeople.value = '';
    if (reviewLocation) reviewLocation.value = '';
    if (reviewDate) reviewDate.value = '';
    hideError(reviewSuccess);
    hideError(reviewError);
    currentReviewType = '';
    currentGeneratedId = '';
}

// Initialize date input to today
if (meetingDate) {
    const today = new Date().toISOString().split('T')[0];
    meetingDate.min = today;
    meetingDate.value = today;
}

showPage(loginPage);
console.log('Website JavaScript loaded successfully!');
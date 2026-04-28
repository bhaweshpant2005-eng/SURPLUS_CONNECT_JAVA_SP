// Surplus Connect - JavaScript Application Logic

// Data Structures and Storage
class SurplusConnect {
    constructor() {
        console.log("SurplusConnect App Version 2.0 Loaded");
        this.sections = ['home', 'donor', 'receiver', 'impact', 'reviews', 'admin'];
        // Initialize data structures
        this.donations = new Map(); // Hash map for quick lookups
        this.requests = new Map();
        this.matches = [];
        this.statistics = {
            totalDonations: 0,
            successfulMatches: 0,
            activeNGOs: 0,
            wastageReduced: "0 tons",
            peopleHelped: 0
        };

        // Categories mapping
        this.categories = {
            "Food": ["Cooked Meals", "Fresh Vegetables", "Packaged Food", "Dairy Products"],
            "Clothes": ["Winter Wear", "Summer Clothes", "Formal Wear", "Children Clothes"],
            "Essentials": ["Medicine", "Books", "Electronics", "Household Items"]
        };

        // Priority queue for urgent requests
        this.urgentRequests = [];

        // Wait for DOM to be ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                this.initializeUI();
                this.bindEvents();
                this.initCursorEffect();
                this.initTheme();
                setTimeout(() => this.initParticles(), 100);
            });
        } else {
            this.initializeUI();
            this.bindEvents();
            this.initCursorEffect();
            this.initTheme();
            setTimeout(() => this.initParticles(), 100);
        }
    }

    // Initialize UI components
    initializeUI() {
        this.updateCategoryOptions();
        this.updateStatistics();
        this.updateUIForAuth(); // Check auth state on load
        // Load real data from backend
        this.fetchDonationsFromBackend();
        this.fetchRequestsFromBackend();
    }

    // Fetch donations from backend and populate local map
    fetchDonationsFromBackend() {
        fetch('/api/items')
            .then(res => res.ok ? res.json() : [])
            .then(items => {
                this.donations.clear();
                items.forEach(item => this.donations.set(item.id, item));
                this.renderDonations();
                this.renderAvailableDonations();
                this.updateStatistics();
            })
            .catch(err => console.error('Failed to load donations:', err));
    }

    // Fetch requests from backend and populate local map
    fetchRequestsFromBackend() {
        fetch('/api/requests')
            .then(res => res.ok ? res.json() : [])
            .then(requests => {
                this.requests.clear();
                requests.forEach(req => this.requests.set(req.id, req));
                this.renderRequests();
                this.renderRecentMatches();
            })
            .catch(err => console.error('Failed to load requests:', err));
    }

    // Bind event listeners
    bindEvents() {
        // Navigation
        const navBtns = document.querySelectorAll('.nav-btn');
        navBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                const section = e.target.dataset.section;
                this.showSection(section);
            });
        });

        // Hero action buttons
        const donateBtn = document.querySelector('[data-action="donate"]');
        const requestBtn = document.querySelector('[data-action="request"]');

        if (donateBtn) {
            donateBtn.addEventListener('click', () => {
                if (localStorage.getItem('token')) {
                    this.showSection('donor');
                } else {
                    document.getElementById('authModal').classList.add('active');
                    document.getElementById('authModalTitle').textContent = 'Please Login to Donate';
                }
            });
        }

        if (requestBtn) {
            requestBtn.addEventListener('click', () => {
                if (localStorage.getItem('token')) {
                    this.showSection('receiver');
                } else {
                    document.getElementById('authModal').classList.add('active');
                    document.getElementById('authModalTitle').textContent = 'Please Login to Request';
                }
            });
        }

        // Form submissions
        const donationForm = document.getElementById('donationForm');
        const requestForm = document.getElementById('requestForm');

        if (donationForm) {
            donationForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleDonationSubmit(e);
            });
        }

        if (requestForm) {
            requestForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleRequestSubmit(e);
            });
        }

        // Item type change handlers
        const itemTypeSelect = document.getElementById('itemType');
        const requestItemTypeSelect = document.getElementById('requestItemType');

        if (itemTypeSelect) {
            itemTypeSelect.addEventListener('change', (e) => {
                this.updateCategorySelect('category', e.target.value);
                this.toggleExpiryField(e.target.value);
            });
        }

        if (requestItemTypeSelect) {
            requestItemTypeSelect.addEventListener('change', (e) => {
                this.updateCategorySelect('requestCategory', e.target.value);
            });
        }

        // Modal close
        const modalClose = document.getElementById('modalClose');
        if (modalClose) {
            modalClose.addEventListener('click', () => {
                this.hideModal();
                // If the modal was an auth error, ensure we're logged out
                const title = document.getElementById('modalTitle').textContent;
                if (title === 'Authorization Needed' || title === 'Session Expired') {
                    this.logout();
                }
            });
        }

        // Star rating UI
        const stars = document.querySelectorAll('.star-rating i');
        stars.forEach(star => {
            star.addEventListener('mouseover', () => {
                const rating = parseInt(star.dataset.rating);
                this.highlightStars(rating);
            });
            star.addEventListener('mouseout', () => {
                this.highlightStars(this.currentRating || 0);
            });
            star.addEventListener('click', () => {
                this.currentRating = parseInt(star.dataset.rating);
                document.getElementById('reviewRating').value = this.currentRating;
                this.highlightStars(this.currentRating);
            });
        });

        // Lookup reviews button
        const lookupBtn = document.getElementById('lookupReviewsBtn');
        if (lookupBtn) {
            lookupBtn.addEventListener('click', () => this.lookupReviews());
        }

        // Review modal close
        const closeReviewModal = document.getElementById('closeReviewModal');
        if (closeReviewModal) {
            closeReviewModal.addEventListener('click', () => {
                document.getElementById('reviewModal').classList.remove('active');
            });
        }

        // Review form submit
        const reviewForm = document.getElementById('reviewForm');
        if (reviewForm) {
            reviewForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleReviewSubmit(e);
            });
        }

        // --- NEW FEATURES EVENT BINDINGS ---

        // Auth Modal Toggles
        const authBtn = document.getElementById('authBtn');
        const closeAuthModal = document.getElementById('closeAuthModal');
        if (authBtn) {
            authBtn.addEventListener('click', () => {
                if(localStorage.getItem('token')) {
                    this.logout();
                } else {
                    document.getElementById('authModalTitle').textContent = 'Login';
                    document.getElementById('authModal').classList.add('active');
                }
            });
        }
        if (closeAuthModal) closeAuthModal.addEventListener('click', () => document.getElementById('authModal').classList.remove('active'));

        // Auth Tabs
        const authTabs = document.querySelectorAll('.auth-tab');
        authTabs.forEach(tab => {
            tab.addEventListener('click', (e) => {
                authTabs.forEach(t => t.classList.remove('active'));
                e.target.classList.add('active');
                const forms = document.querySelectorAll('.auth-form');
                forms.forEach(f => f.style.display = 'none');
                document.getElementById(e.target.dataset.tab + 'Form').style.display = 'block';
            });
        });

        // Login Submit
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleLogin();
            });
        }

        // Register Submit
        const registerForm = document.getElementById('registerForm');
        if (registerForm) {
            registerForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleRegister();
            });
        }

        // Notifications Modal Toggle
        const notificationBtn = document.getElementById('notificationBtn');
        const closeNotifModal = document.getElementById('closeNotifModal');
        if (notificationBtn) {
            notificationBtn.addEventListener('click', () => {
                document.getElementById('notificationsModal').classList.add('active');
                this.fetchNotifications();
            });
        }
        if (closeNotifModal) closeNotifModal.addEventListener('click', () => document.getElementById('notificationsModal').classList.remove('active'));

    }

    // Show specific section
    showSection(sectionName) {
        // Update navigation
        const navBtns = document.querySelectorAll('.nav-btn');
        navBtns.forEach(btn => {
            btn.classList.remove('active');
            if (btn.dataset.section === sectionName) {
                btn.classList.add('active');
            }
        });

        // Update sections
        const sections = document.querySelectorAll('.section');
        sections.forEach(section => {
            section.classList.remove('active');
        });

        const activeSection = document.getElementById(sectionName);
        if (activeSection) {
            activeSection.classList.add('active');
            activeSection.classList.add('fade-in');
            
            // If admin section is loaded, fetch admin data
            if(sectionName === 'admin') {
                this.loadAdminDashboard();
            }
            // If reviews section, load completed allocations
            if(sectionName === 'reviews') {
                this.loadCompletedAllocations();
            }
            // If impact/statistics section, load real stats
            if(sectionName === 'statistics') {
                this.loadImpactStats();
            }
        }
    }

    // Update category options based on item type
    updateCategoryOptions() {
        const categorySelect = document.getElementById('category');
        const requestCategorySelect = document.getElementById('requestCategory');

        if (categorySelect) categorySelect.innerHTML = '<option value="">Select Category</option>';
        if (requestCategorySelect) requestCategorySelect.innerHTML = '<option value="">Select Category</option>';
    }

    updateCategorySelect(selectId, itemType) {
        const select = document.getElementById(selectId);
        if (!select) return;

        select.innerHTML = '<option value="">Select Category</option>';

        if (itemType && this.categories[itemType]) {
            this.categories[itemType].forEach(category => {
                const option = document.createElement('option');
                option.value = category;
                option.textContent = category;
                select.appendChild(option);
            });
        }
    }

    // Toggle expiry field for food items
    toggleExpiryField(itemType) {
        const expiryGroup = document.getElementById('expiryGroup');
        if (!expiryGroup) return;

        if (itemType === 'Food') {
            expiryGroup.style.display = 'block';
            document.getElementById('expiryDate').required = true;
        } else {
            expiryGroup.style.display = 'none';
            document.getElementById('expiryDate').required = false;
        }
    }

    // Handle donation form submission
    handleDonationSubmit(e) {
        
        const donation = {
            donorName: document.getElementById('donorName').value,
            donorPhone: document.getElementById('donorPhone').value,
            itemType: document.getElementById('itemType').value,
            category: document.getElementById('category').value,
            quantity: document.getElementById('quantity').value,
            expiryDate: document.getElementById('expiryDate').value || null,
            location: document.getElementById('location').value,
            status: 'Available',
            timestamp: new Date().toISOString(),
            priority: document.getElementById('itemType').value === 'Food' ? 'High' : 'Medium'
        };

        // Check auth
        if (!localStorage.getItem('token')) {
            document.getElementById('authModal').classList.add('active');
            document.getElementById('authModalTitle').textContent = 'Please Login to Donate';
            return;
        }

        if (localStorage.getItem('role') !== 'DONOR') {
            alert('Access denied. Only Donors can register donations.');
            return;
        }

        // Send to SQL backend
        this.authFetch('/api/items', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(donation)
        })
        .then(response => {
            if (response.status === 401 || response.status === 403) {
                throw new Error('AUTH_ERROR');
            }
            if (response.status === 409) return response.text().then(t => { throw new Error(t); });
            if (!response.ok) return response.text().then(t => { throw new Error(t || 'SERVER_ERROR'); });
            return response.json();
        })
        .then(data => {
            // Feature 22: Image Upload
            const imageFile = document.getElementById('itemImage').files[0];
            if (imageFile) {
                const formData = new FormData();
                formData.append('file', imageFile);

                return this.authFetch(`/api/items/${data.id}/upload-image`, {
                    method: 'POST',
                    body: formData
                })
                .then(resp => {
                    if (!resp.ok) throw new Error('Image upload failed');
                    return resp.json();
                })
                .catch(err => {
                    console.error(err);
                    return data; // Proceed with the saved item even if image fails
                });
            }
            return data;
        })
        .then(finalData => {
            // Reload all donations from backend to get updated quantities after matching
            this.fetchDonationsFromBackend();

            // Update statistics
            this.statistics.totalDonations++;
            this.updateStatistics();

            // Reset form
            e.target.reset();
            this.updateCategoryOptions();

            // Show success modal
            this.showModal('Donation Registered!', 'Your surplus item has been registered successfully. Matching is in progress.');
        })
        .catch(error => {
            console.error('Error saving donation:', error);
            if (error.message === 'AUTH_ERROR') {
                this.showModal('Authorization Needed', 'Your session has expired or you do not have permission. Please login as a Donor.', 'error');
                this.logout();
                document.getElementById('authModal').classList.add('active');
            } else {
                this.showModal('Registration Failed', 'Failed to register donation. Please ensure the backend is running and you are logged in correctly.', 'error');
            }
        });
    }

    // Handle request form submission
    handleRequestSubmit(e) {
        const request = {
            ngoName: document.getElementById('ngoName').value,
            contactPerson: document.getElementById('contactPerson').value,
            phone: document.getElementById('ngoPhone').value,
            itemType: document.getElementById('requestItemType').value,   // mapped to resourceType in backend
            category: document.getElementById('requestCategory').value,
            quantity: document.getElementById('requestQuantity').value,   // mapped to quantityRequested in backend
            urgency: document.getElementById('urgency').value,            // mapped to urgencyLevel in backend
            location: document.getElementById('requestLocation').value,
            status: 'Pending'
        };

        // Check auth
        if (!localStorage.getItem('token')) {
            document.getElementById('authModal').classList.add('active');
            document.getElementById('authModalTitle').textContent = 'Please Login to Request';
            return;
        }

        if (localStorage.getItem('role') !== 'NGO' && localStorage.getItem('role') !== 'ADMIN') {
            this.showModal('Access Denied', 'Only NGOs can submit resource requests.', 'error');
            return;
        }

        // Send to backend
        this.authFetch('/api/requests', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        })
        .then(res => {
            if (res.status === 409) return res.text().then(t => { throw new Error(t); });
            if (!res.ok) return res.text().then(t => { throw new Error(t || 'Failed to submit request'); });
            return res.json();
        })
        .then(finalData => {
            // Reload from backend to get updated matching status
            this.fetchDonationsFromBackend();
            this.fetchRequestsFromBackend();
            e.target.reset();
            this.updateCategoryOptions();
            this.showModal('Request Submitted!', 'Your request has been submitted. Matching is in progress.');
        })
        .catch(err => {
            console.error(err);
            this.showModal('Request Failed', err.message || 'Failed to submit request.', 'error');
        });
    }

    // Matching algorithm - matches donations with requests
    findMatches() {
        const availableDonations = Array.from(this.donations.values())
            .filter(d => d.status === 'Available');

        const pendingRequests = Array.from(this.requests.values())
            .filter(r => r.status === 'Pending');

        // Sort requests by urgency (High priority first)
        pendingRequests.sort((a, b) => {
            const urgencyOrder = { 'High': 3, 'Medium': 2, 'Low': 1 };
            return urgencyOrder[b.urgency] - urgencyOrder[a.urgency];
        });

        for (const request of pendingRequests) {
            for (const donation of availableDonations) {
                if (this.isMatch(donation, request)) {
                    // Create match
                    const match = {
                        id: Date.now() + Math.random(),
                        donationId: donation.id,
                        requestId: request.id,
                        matchedAt: new Date().toISOString(),
                        status: 'Confirmed'
                    };

                    this.matches.push(match);

                    // Update statuses
                    donation.status = 'Matched';
                    request.status = 'Matched';

                    // Update statistics
                    this.statistics.successfulMatches++;
                    this.statistics.peopleHelped += 10; // Estimated people helped per match

                    break;
                }
            }
        }

        // Re-render affected components
        this.renderDonations();
        this.renderRequests();
        this.renderAvailableDonations();
        this.updateStatistics();
        this.renderRecentMatches();
    }

    // Check if donation matches request
    isMatch(donation, request) {
        return donation.itemType === request.itemType &&
               donation.category === request.category &&
               donation.location.toLowerCase().includes(request.location.toLowerCase()) &&
               donation.status === 'Available' &&
               request.status === 'Pending';
    }

    // Render donations list
    renderDonations() {
        const container = document.getElementById('donationsList');
        if (!container) return;

        const userDonations = Array.from(this.donations.values())
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        if (userDonations.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 2rem;">No donations yet. Register your first donation!</p>';
            return;
        }

        container.innerHTML = userDonations.map(donation => `
            <div class="item-card slide-in">
                <div class="item-header">
                    <div class="item-title">${donation.category}</div>
                    <div class="item-status status-${donation.status.toLowerCase().replace(/\s+/g, '-')}">${donation.status}</div>
                </div>
                <div class="item-details-flex" style="display: flex; gap: 15px; margin-top: 10px;">
                    ${donation.imageUrl ? `<img src="${donation.imageUrl}" alt="Item image" style="width: 100px; height: 100px; object-fit: cover; border-radius: 8px;">` : ''}
                    <div class="item-details" style="flex-grow: 1;">
                        <div><strong>Type:</strong> ${donation.itemType}</div>
                        <div><strong>Total Qty:</strong> ${donation.originalQuantityNum || donation.quantity}</div>
                        <div><strong>Allocated:</strong> ${(donation.originalQuantityNum || 0) - (donation.remainingQuantityNum !== undefined ? donation.remainingQuantityNum : (donation.originalQuantityNum || 0))}</div>
                        <div><strong>Remaining:</strong> ${donation.remainingQuantityNum !== undefined ? donation.remainingQuantityNum : donation.quantity}</div>
                        <div><strong>Location:</strong> ${donation.location}</div>
                        ${donation.expiryDate ? `<div><strong>Expires:</strong> ${new Date(donation.expiryDate).toLocaleDateString()}</div>` : ''}
                        <div><strong>Donated:</strong> ${new Date(donation.timestamp).toLocaleDateString()}</div>
                    </div>
                </div>
            </div>
        `).join('');
    }

    // Render requests list
    renderRequests() {
        const container = document.getElementById('requestsList');
        if (!container) return;

        const userRequests = Array.from(this.requests.values())
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        if (userRequests.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 2rem;">No requests yet. Submit your first request!</p>';
            return;
        }

        container.innerHTML = userRequests.map(request => {
            const urgency = request.urgencyLevel || request.urgency || 'NORMAL';
            const urgencyDisplay = urgency.charAt(0) + urgency.slice(1).toLowerCase();
            const qty = request.quantityRequested || request.quantity || 0;
            const remaining = request.remainingNeed !== undefined ? request.remainingNeed : qty;
            const fulfilled = qty - remaining;
            const itemType = request.resourceType || request.itemType || 'N/A';
            const category = request.category || 'N/A';
            return `
            <div class="item-card slide-in">
                <div class="item-header">
                    <div class="item-title">${itemType} — ${category}</div>
                    <div class="item-status status-${(request.status || 'pending').toLowerCase().replace(/\s+/g, '-')}">${request.status || 'Pending'}</div>
                </div>
                <div class="item-details">
                    <div><strong>NGO:</strong> ${request.ngoName || 'N/A'}</div>
                    <div><strong>Requested:</strong> ${qty}</div>
                    <div><strong>Fulfilled:</strong> ${fulfilled}</div>
                    <div><strong>Remaining Need:</strong> ${remaining}</div>
                    <div><strong>Urgency:</strong> <span class="item-status status-${urgency.toLowerCase()}">${urgencyDisplay}</span></div>
                    <div><strong>Location:</strong> ${request.location || 'N/A'}</div>
                </div>
            </div>
        `}).join('');
    }

    // Render available donations for receivers
    renderAvailableDonations() {
        const container = document.getElementById('availableDonations');
        if (!container) return;

        const availableDonations = Array.from(this.donations.values())
            .filter(d => d.status === 'Available')
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        if (availableDonations.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 2rem;">No available donations at the moment.</p>';
            return;
        }

        container.innerHTML = availableDonations.map(donation => `
            <div class="item-card slide-in">
                <div class="item-header">
                    <div class="item-title">${donation.category}</div>
                    <div class="item-status status-${donation.status.toLowerCase().replace(/\s+/g, '-')}">${donation.status}</div>
                </div>
                <div class="item-details-flex" style="display: flex; gap: 15px; margin-top: 10px;">
                    ${donation.imageUrl ? `<img src="${donation.imageUrl}" alt="Item image" style="width: 100px; height: 100px; object-fit: cover; border-radius: 8px;">` : ''}
                    <div class="item-details" style="flex-grow: 1;">
                        <div><strong>Donor:</strong> ${donation.donorName}</div>
                        <div><strong>Type:</strong> ${donation.itemType}</div>
                        <div><strong>Total Qty:</strong> ${donation.originalQuantityNum || donation.quantity}</div>
                        <div><strong>Available Qty:</strong> ${donation.remainingQuantityNum !== undefined ? donation.remainingQuantityNum : donation.quantity}</div>
                        <div><strong>Location:</strong> ${donation.location}</div>
                        <div><strong>Contact:</strong> ${donation.donorPhone}</div>
                        ${donation.expiryDate ? `<div><strong>Expires:</strong> ${new Date(donation.expiryDate).toLocaleDateString()}</div>` : ''}
                    </div>
                </div>
            </div>
        `).join('');
    }

    // Update statistics display
    updateStatistics() {
        const elements = {
            'totalDonations': this.statistics.totalDonations,
            'successfulMatches': this.statistics.successfulMatches,
            'activeNGOs': this.statistics.activeNGOs,
            'wastageReduced': this.statistics.wastageReduced,
            'peopleHelped': this.statistics.peopleHelped.toLocaleString()
        };

        Object.entries(elements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value;
            }
        });
    }

    // Render recent matches
    renderRecentMatches() {
        const container = document.getElementById('recentMatches');
        if (!container) return;

        const recentMatches = this.matches
            .sort((a, b) => new Date(b.matchedAt) - new Date(a.matchedAt))
            .slice(0, 5);

        if (recentMatches.length === 0) {
            container.innerHTML = '<p style="text-align: center; color: var(--text-light); padding: 2rem;">No matches yet.</p>';
            return;
        }

        container.innerHTML = recentMatches.map(match => {
            const donation = this.donations.get(match.donationId);
            const request = this.requests.get(match.requestId);

            if (!donation || !request) return '';

            return `
                <div class="match-card slide-in">
                    <div class="match-header">${donation.category} → ${request.ngoName}</div>
                    <div class="match-details">
                        <div>Quantity: ${donation.quantity}</div>
                        <div>Location: ${donation.location}</div>
                        <div>Matched: ${new Date(match.matchedAt).toLocaleDateString()}</div>
                    </div>
                </div>
            `;
        }).join('');
    }

    // Highlight stars based on rating (hover or selected)
    highlightStars(rating) {
        const stars = document.querySelectorAll('.star-rating i');
        stars.forEach(star => {
            const starRating = parseInt(star.dataset.rating);
            if (starRating <= rating) {
                star.classList.add('active');
                star.classList.remove('hovered');
            } else {
                star.classList.remove('active');
                star.classList.remove('hovered');
            }
        });
    }

    // Submit a new review to the backend
    handleReviewSubmit(e) {
        const review = {
            allocationId: document.getElementById('reviewAllocationId').value,
            donationId: document.getElementById('reviewDonationId').value,
            reviewerName: document.getElementById('reviewerName').value,
            reviewerType: document.getElementById('reviewerType').value,
            targetId: document.getElementById('reviewTargetId').value,
            targetType: document.getElementById('reviewTargetType').value,
            rating: parseInt(document.getElementById('reviewRating').value) || 0,
            comment: document.getElementById('reviewComment').value
        };

        if (!review.reviewerName || review.rating === 0) {
            alert('Please enter your name and select a rating.');
            return;
        }

        if (!review.targetId) {
            alert('Target ID is missing. Please try again.');
            return;
        }

        this.authFetch('/api/reviews', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(review)
        })
        .then(resp => {
            if (!resp.ok) throw new Error('Network error or Duplicate review detected');
            return resp.json();
        })
        .then(data => {
            const imageFile = document.getElementById('reviewImage').files[0];
            if (imageFile) {
                const formData = new FormData();
                formData.append('file', imageFile);

                return this.authFetch(`/api/reviews/${data.id}/upload-image`, {
                    method: 'POST',
                    body: formData
                })
                .then(imgResp => {
                    if (!imgResp.ok) throw new Error('Image upload failed');
                    return imgResp.json();
                })
                .catch(err => {
                    console.error(err);
                    return data; // Proceed without image if it fails
                });
            }
            return data;
        })
        .then(finalData => {
            // Reset form & rating UI
            e.target.reset();
            this.currentRating = 0;
            this.highlightStars(0);
            document.getElementById('reviewRating').value = '';
            document.getElementById('reviewModal').classList.remove('active');
            this.showModal('Review Submitted!', 'Your review has been recorded successfully.');
            // Only refresh lookup if a target is already searched
            const lookupId = document.getElementById('lookupTargetId').value;
            if (lookupId) this.lookupReviews();
        })
        .catch(err => {
            console.error(err);
            alert('Failed to submit review. It may be a duplicate.');
        });
    }

    // Lookup and display reviews for a target
    lookupReviews() {
        const targetId = document.getElementById('lookupTargetId').value;
        const targetType = document.getElementById('lookupTargetType').value;
        if (!targetId || !targetType) {
            alert('Please provide both Target ID and Type.');
            return;
        }
        // Correct endpoint: /api/reviews/target/{targetId}?targetType=NGO
        fetch(`/api/reviews/target/${targetId}?targetType=${encodeURIComponent(targetType)}`)
            .then(resp => {
                if (!resp.ok) throw new Error('Network error');
                return resp.json();
            })
            .then(data => {
                this.renderReviews(data);
                this.updateReviewStats(data);
            })
            .catch(err => {
                console.error(err);
                alert('Failed to fetch reviews.');
            });
    }

    // Render reviews list
    renderReviews(reviews) {
        const container = document.getElementById('reviewsList');
        if (!container) return;
        if (!reviews || reviews.length === 0) {
            container.innerHTML = '<p style="text-align:center;color:var(--text-light);padding:2rem;">No reviews found.</p>';
            return;
        }
        container.innerHTML = reviews.map(r => `
            <div class="review-card">
                <div class="review-header">
                    <span class="review-author">${r.reviewerName} (${r.reviewerType})</span>
                    <span class="review-stars">${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</span>
                </div>
                ${r.imageUrl ? `<div style="margin: 10px 0;"><img src="${r.imageUrl}" alt="Review proof" style="max-width: 100%; max-height: 200px; border-radius: 8px;"></div>` : ''}
                <div class="review-comment">${r.comment || ''}</div>
                <div class="review-date">${new Date(r.createdAt).toLocaleDateString()}</div>
            </div>
        `).join('');
    }

    // Update average rating and total count
    updateReviewStats(reviews) {
        const statsCard = document.getElementById('reviewStats');
        if (!statsCard) return;
        if (!reviews || reviews.length === 0) {
            statsCard.style.display = 'none';
            return;
        }
        const total = reviews.length;
        const avg = (reviews.reduce((sum, r) => sum + r.rating, 0) / total).toFixed(1);
        statsCard.innerHTML = `
            <div class="avg-rating">${avg}</div>
            <div class="avg-stars">${'★'.repeat(Math.round(avg))}</div>
            <div class="total-reviews">${total} review(s)</div>
        `;
        statsCard.style.display = 'block';
    }

    // --- NEW METHODS FOR AUTH, NOTIFICATIONS, ADMIN ---

    authFetch(url, options = {}) {
        const token = localStorage.getItem('token');
        if (token) {
            if(!options.headers) options.headers = {};
            options.headers['Authorization'] = 'Bearer ' + token;
        }
        return fetch(url, options).then(response => {
            if (response.status === 401 || response.status === 403) {
                // Token invalid or unauthorized
                const role = localStorage.getItem('role');
                if (response.status === 401) {
                    localStorage.clear();
                    // We don't reload here to avoid interrupting the user, 
                    // but they will be prompted on next auth action
                }
            }
            return response;
        });
    }

    handleLogin() {
        const username = document.getElementById('loginUsername').value;
        const password = document.getElementById('loginPassword').value;

        fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        })
        .then(res => {
            if (!res.ok) throw new Error('Login failed');
            return res.json();
        })
        .then(data => {
            localStorage.setItem('token', data.token);
            localStorage.setItem('role', data.role);
            localStorage.setItem('userId', data.userId);
            localStorage.setItem('username', data.username);
            
            document.getElementById('authModal').classList.remove('active');
            this.updateUIForAuth();
            this.showModal('Success', 'Logged in successfully!');
        })
        .catch(err => this.showModal('Login Failed', 'Invalid username or password. Please try again.', 'error'));
    }

    handleRegister() {
        const user = {
            username: document.getElementById('regUsername').value,
            email: document.getElementById('regEmail').value,
            password: document.getElementById('regPassword').value,
            role: document.getElementById('regRole').value
        };

        fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(user)
        })
        .then(async res => {
            const text = await res.text();
            if (!res.ok) {
                console.error('Server error response:', text);
                throw new Error(text || 'Registration failed');
            }
            // Close auth modal first, then show success message
            document.getElementById('authModal').classList.remove('active');
            // Switch auth modal to login tab for next time user opens it
            const loginTab = document.querySelector('.auth-tab[data-tab="login"]');
            if (loginTab) {
                document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
                loginTab.classList.add('active');
                document.querySelectorAll('.auth-form').forEach(f => f.style.display = 'none');
                document.getElementById('loginForm').style.display = 'block';
            }
            this.showModal('Registration Successful!', 'Your account has been created. Please login to continue.');
        })
        .catch(err => {
            console.error('Registration error:', err);
            this.showModal('Registration Failed', 
                err.message === 'Registration failed' ? 'Registration failed. Try a different username/email.' : err.message, 
                'error'
            );
        });
    }

    logout() {
        localStorage.clear();
        this.updateUIForAuth();
        this.showSection('home');
    }

    updateUIForAuth() {
        const token = localStorage.getItem('token');
        const role = localStorage.getItem('role');
        const authBtn = document.getElementById('authBtn');
        const notifBtn = document.getElementById('notificationBtn');
        const adminBtns = document.querySelectorAll('.admin-only');
        const donorBtns = document.querySelectorAll('.donor-only');
        const ngoBtns = document.querySelectorAll('.ngo-only');

        if (token) {
            authBtn.textContent = 'Logout';
            notifBtn.style.display = 'inline-block';
            this.fetchNotifications();
            
            // Role based navigation
            if (role === 'ADMIN') {
                adminBtns.forEach(btn => btn.style.display = 'inline-block');
                donorBtns.forEach(btn => btn.style.display = 'inline-block');
                ngoBtns.forEach(btn => btn.style.display = 'inline-block');
            } else if (role === 'DONOR') {
                adminBtns.forEach(btn => btn.style.display = 'none');
                donorBtns.forEach(btn => btn.style.display = 'inline-block');
                ngoBtns.forEach(btn => btn.style.display = 'none');
            } else if (role === 'NGO') {
                adminBtns.forEach(btn => btn.style.display = 'none');
                donorBtns.forEach(btn => btn.style.display = 'none');
                ngoBtns.forEach(btn => btn.style.display = 'inline-block');
            }
        } else {
            authBtn.textContent = 'Login';
            notifBtn.style.display = 'none';
            adminBtns.forEach(btn => btn.style.display = 'none');
            donorBtns.forEach(btn => btn.style.display = 'none');
            ngoBtns.forEach(btn => btn.style.display = 'none');
        }
    }

    fetchNotifications() {
        const userId = localStorage.getItem('userId');
        if (!userId) return;

        this.authFetch(`/api/notifications/${userId}`)
            .then(res => res.json())
            .then(data => {
                document.getElementById('notificationCount').textContent = data.length;
                const list = document.getElementById('notificationsList');
                if (data.length === 0) {
                    list.innerHTML = '<p>No new notifications</p>';
                } else {
                    list.innerHTML = data.map(n => `
                        <div style="padding: 10px; border-bottom: 1px solid #ddd; display: flex; justify-content: space-between;">
                            <div>
                                <strong>${n.type}</strong><br>
                                ${n.message}<br>
                                <small>${new Date(n.timestamp).toLocaleString()}</small>
                            </div>
                            <button onclick="surplusConnect.markNotifRead(${n.id})" style="background:var(--primary-color);color:white;border:none;border-radius:4px;cursor:pointer;">Mark Read</button>
                        </div>
                    `).join('');
                }
            })
            .catch(console.error);
    }

    markNotifRead(id) {
        this.authFetch(`/api/notifications/${id}/read`, { method: 'POST' })
            .then(() => {
                this.fetchNotifications();
            });
    }

    loadAdminDashboard() {
        this.authFetch('/api/admin/analytics')
            .then(res => res.json())
            .then(data => {
                document.getElementById('adminTotalDonations').textContent = data.totalDonations;
                document.getElementById('adminTotalRequests').textContent = data.totalRequests;
                document.getElementById('adminSuccessfulMatches').textContent = data.successfulMatches;
            });

        this.authFetch('/api/admin/leaderboard')
            .then(res => res.json())
            .then(data => {
                const board = document.getElementById('ngoLeaderboard');
                if (data.length === 0) {
                    board.innerHTML = '<p>No active NGOs yet.</p>';
                    return;
                }
                board.innerHTML = data.map((ngo, index) => `
                    <div class="item-card slide-in">
                        <div class="item-header">
                            <div><strong>#${index + 1}</strong> ${ngo.name}</div>
                            <div class="item-status status-matched">Rating: ${ngo.rating} ★</div>
                        </div>
                        <div class="item-details">
                            <div>Category: ${ngo.ngoCategory || 'N/A'}</div>
                            <div>Total Received: ${ngo.totalReceived}</div>
                        </div>
                    </div>
                `).join('');
            });
    }

    runMatchingNow() {
        // Fetch all available items and trigger matching for each
        fetch('/api/items')
            .then(r => r.json())
            .then(items => {
                const available = items.filter(i =>
                    i.status === 'Available' || i.status === 'PartiallyAllocated');
                if (available.length === 0) {
                    this.showModal('No Items', 'No available donations to match.', 'error');
                    return;
                }
                const promises = available.map(item =>
                    this.authFetch('/api/matching/split/' + item.id, {method: 'POST'})
                        .then(r => r.json())
                        .catch(() => [])
                );
                Promise.all(promises).then(results => {
                    const total = results.reduce((sum, r) => sum + (Array.isArray(r) ? r.length : 0), 0);
                    this.fetchDonationsFromBackend();
                    this.fetchRequestsFromBackend();
                    this.showModal('Matching Complete', total + ' allocation(s) created successfully.');
                });
            });
    }

    loadImpactStats() {
        // Load real stats from backend
        this.authFetch('/api/admin/analytics')
            .then(r => r.json())
            .then(data => {
                const td = document.getElementById('totalDonations');
                const sm = document.getElementById('successfulMatches');
                if (td) td.textContent = data.totalDonations || 0;
                if (sm) sm.textContent = data.successfulMatches || 0;
            }).catch(() => {});

        // Load NGO impact data
        fetch('/api/ngos')
            .then(r => r.json())
            .then(ngos => {
                let totalPeople = 0, totalWaste = 0;
                ngos.forEach(n => {
                    totalPeople += n.peopleHelped || 0;
                    totalWaste += n.wasteReducedKg || 0;
                });
                const ph = document.getElementById('peopleHelped');
                const wr = document.getElementById('wastageReduced');
                if (ph) ph.textContent = totalPeople.toLocaleString();
                if (wr) wr.textContent = (totalWaste / 1000).toFixed(1) + 'T';

                // Active NGOs count
                const an = document.getElementById('activeNGOs');
                if (an) an.textContent = ngos.filter(n => (n.totalReceived || 0) > 0).length;
            }).catch(() => {});

        // Category distribution
        fetch('/api/items')
            .then(r => r.json())
            .then(items => {
                const counts = {Food: 0, Clothes: 0, Essentials: 0};
                items.forEach(i => { if (counts[i.itemType] !== undefined) counts[i.itemType]++; });
                const total = items.length || 1;
                Object.keys(counts).forEach(type => {
                    const pct = Math.round((counts[type] / total) * 100);
                    const bar = document.querySelector('[data-category="' + type + '"]');
                    if (bar) {
                        bar.style.width = pct + '%';
                        bar.closest('.category-item').querySelector('.category-value').textContent = pct + '%';
                    }
                });
            }).catch(() => {});
    }

    loadCompletedAllocations() {
        const container = document.getElementById('completedAllocations');
        if (!container) return;

        const role = localStorage.getItem('role');
        const userId = localStorage.getItem('userId');

        if (!userId) {
            container.innerHTML = '<p style="text-align:center;color:var(--text-light);padding:2rem;">Please login to see your transactions.</p>';
            return;
        }

        // Fetch all allocations
        // Load allocations from backend via items
        fetch('/api/items')
            .then(r => r.json())
            .then(items => {
                const myItems = role === 'DONOR'
                    ? items.filter(i => String(i.donorId) === String(userId) &&
                        (i.status === 'PartiallyAllocated' || i.status === 'FullyAllocated' || i.status === 'Matched'))
                    : items.filter(i => i.status === 'PartiallyAllocated' || i.status === 'FullyAllocated' || i.status === 'Matched');

                if (myItems.length === 0) {
                    container.innerHTML = '<p style="text-align:center;color:var(--text-light);padding:2rem;">No completed transactions yet.</p>';
                    return;
                }

                container.innerHTML = myItems.map(item => {
                    const allocated = (item.originalQuantityNum || 0) - (item.remainingQuantityNum || 0);
                    return '<div class="item-card">' +
                        '<div class="item-header">' +
                        '<div class="item-title">' + item.itemType + ' - ' + item.category + '</div>' +
                        '<div class="item-status status-matched">' + item.status + '</div>' +
                        '</div>' +
                        '<div class="item-details" style="margin-bottom:10px;">' +
                        '<div><strong>Donor:</strong> ' + (item.donorName || 'N/A') + '</div>' +
                        '<div><strong>Qty:</strong> ' + item.originalQuantityNum + ' | <strong>Allocated:</strong> ' + allocated + '</div>' +
                        '<div><strong>Location:</strong> ' + item.location + '</div>' +
                        '</div>' +
                        '<button class="btn btn-secondary" style="width:100%;margin-top:8px;" ' +
                        'onclick="surplusConnect.openReviewModal(' + item.id + ', \'' + item.donorId + '\', \'' + role + '\')">' +
                        '<i class="fas fa-star"></i> Leave a Review' +
                        '</button></div>';
                }).join('');
            })
            .catch(function() {
                container.innerHTML = '<p style="text-align:center;color:var(--text-light);padding:2rem;">Could not load transactions.</p>';
            });
    }

    openReviewModal(itemId, donorId, role) {
        const userId = localStorage.getItem('userId');

        // Set hidden fields
        document.getElementById('reviewDonationId').value = itemId;
        document.getElementById('reviewAllocationId').value = itemId;
        document.getElementById('reviewerType').value = role;

        // If donor → reviewing NGO (targetId = ngoId from allocation)
        // If NGO → reviewing Donor
        if (role === 'DONOR') {
            document.getElementById('reviewTargetId').value = userId; // will be overridden below
            document.getElementById('reviewTargetType').value = 'NGO';
            // Try to get the NGO id from allocations
            fetch(`/api/matching/queues`)
                .catch(() => {});
            // Use itemId as targetId proxy for NGO lookup
            document.getElementById('reviewTargetId').value = itemId;
        } else {
            document.getElementById('reviewTargetId').value = donorId || userId;
            document.getElementById('reviewTargetType').value = 'DONOR';
        }

        // Pre-fill reviewer name
        const username = localStorage.getItem('username') || '';
        document.getElementById('reviewerName').value = username;

        // Show transaction info
        document.getElementById('reviewTransactionInfo').innerHTML = `
            <div><strong>Item ID:</strong> ${itemId}</div>
            <div><strong>You are reviewing:</strong> ${role === 'DONOR' ? 'the NGO' : 'the Donor'}</div>
        `;

        // Reset stars
        this.currentRating = 0;
        this.highlightStars(0);
        document.getElementById('reviewRating').value = '0';
        document.getElementById('reviewComment').value = '';

        document.getElementById('reviewModal').classList.add('active');
    }

    showModal(title, message, type = 'success') {
        const icon = document.getElementById('modalIcon');
        const titleEl = document.getElementById('modalTitle');
        
        if (icon) {
            icon.className = type === 'success' ? 'fas fa-check-circle' : 'fas fa-exclamation-triangle';
        }
        
        document.getElementById('modalTitle').textContent = title;
        document.getElementById('modalMessage').textContent = message;
        document.getElementById('successModal').classList.add('active');
    }

    hideModal() {
        document.getElementById('successModal').classList.remove('active');
    }

    initCursorEffect() {
        let lastX = 0;
        let lastY = 0;
        const threshold = 15; // Only spawn if moved more than 15px

        document.addEventListener('mousemove', (e) => {
            const dist = Math.hypot(e.clientX - lastX, e.clientY - lastY);
            if (dist < threshold) return;

            lastX = e.clientX;
            lastY = e.clientY;

            const bubble = document.createElement('div');
            bubble.className = 'cursor-bubble';
            bubble.style.left = e.clientX + 'px';
            bubble.style.top = e.clientY + 'px';
            
            document.body.appendChild(bubble);

            // Remove bubble after animation ends
            setTimeout(() => {
                bubble.remove();
            }, 800);
        });
    }

    initTheme() {
        const theme = localStorage.getItem('theme') || 'light';
        document.documentElement.setAttribute('data-theme', theme);
        this.updateThemeIcon(theme);

        const themeBtn = document.getElementById('themeToggle');
        if (themeBtn) {
            themeBtn.addEventListener('click', () => {
                const currentTheme = document.documentElement.getAttribute('data-theme');
                const newTheme = currentTheme === 'light' ? 'dark' : 'light';
                document.documentElement.setAttribute('data-theme', newTheme);
                localStorage.setItem('theme', newTheme);
                this.updateThemeIcon(newTheme);
                // Update particle color without full reinit
                this.updateParticleColor(newTheme);
            });
        }
    }

    updateParticleColor(theme) {
        const color = theme === 'light' ? '#a0aec0' : '#cbd5e1';
        try {
            if (window.pJSDom && window.pJSDom.length > 0) {
                const p = window.pJSDom[0].pJS;
                p.particles.color.value = color;
                p.particles.line_linked.color = color;
                p.fn.particlesRefresh();
            }
        } catch (e) {
            // particles not ready, ignore
        }
    }

    updateThemeIcon(theme) {
        const icon = document.querySelector('#themeToggle i');
        if (icon) {
            icon.className = theme === 'light' ? 'fas fa-moon' : 'fas fa-sun';
        }
    }

    initParticles() {
        const theme = document.documentElement.getAttribute('data-theme') || 'light';
        const color = theme === 'light' ? '#a0aec0' : '#cbd5e1';
        
        if (typeof particlesJS !== 'undefined') {
            particlesJS('particles-js', {
                "particles": {
                    "number": { "value": 100, "density": { "enable": true, "value_area": 800 } },
                    "color": { "value": color },
                    "shape": { "type": "circle" },
                    "opacity": { "value": 0.4, "random": false },
                    "size": { "value": 2, "random": true },
                    "line_linked": { "enable": true, "distance": 150, "color": color, "opacity": 0.3, "width": 1 },
                    "move": { "enable": true, "speed": 1.5, "direction": "none", "random": false, "straight": false, "out_mode": "out", "bounce": false }
                },
                "interactivity": {
                    "detect_on": "canvas",
                    "events": { "onhover": { "enable": true, "mode": "grab" }, "onclick": { "enable": true, "mode": "push" }, "resize": true },
                    "modes": { "grab": { "distance": 200, "line_linked": { "opacity": 0.6 } }, "push": { "particles_nb": 4 } }
                },
                "retina_detect": true
            });
        } else {
            console.warn('particlesJS not found, retrying...');
            setTimeout(() => this.initParticles(), 500);
        }
    }

}

// Initialize the application
const surplusConnect = new SurplusConnect();
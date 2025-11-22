import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './components/Login';
import Signup from './components/Signup';
import Home from './components/Home';
import Meeting from './components/Meeting';
import Email from './components/Email';
import Review from './components/Review';
import { Page, ReviewData } from './types';
import './styles/App.css';

const AppContent: React.FC = () => {
  const { user, loading } = useAuth();
  const [currentPage, setCurrentPage] = useState<Page>('login');
  const [reviewData, setReviewData] = useState<ReviewData | null>(null);

  useEffect(() => {
    if (!loading) {
      if (user) {
        setCurrentPage('home');
      } else {
        setCurrentPage('login');
      }
    }
  }, [user, loading]);

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="spinner"></div>
      </div>
    );
  }

  const navigateToReview = (data: ReviewData): void => {
    setReviewData(data);
    setCurrentPage('review');
  };

  const navigateToPage = (page: Page): void => {
    if (currentPage === 'review' && (page === 'email' || page === 'meeting')) {
      // Keep reviewData for editing
    } else if (page !== 'review') {
      setReviewData(null);
    }
    setCurrentPage(page);
  };

  return (
    <div className="app">
      <div className="background-animation">
        <div className="gradient-orb orb-1"></div>
        <div className="gradient-orb orb-2"></div>
        <div className="gradient-orb orb-3"></div>
      </div>

      {currentPage === 'login' && <Login onNavigate={navigateToPage} />}
      {currentPage === 'signup' && <Signup onNavigate={navigateToPage} />}
      {currentPage === 'home' && <Home onNavigate={navigateToPage} />}
      {currentPage === 'meeting' && <Meeting onNavigate={navigateToPage} onReview={navigateToReview} reviewData={reviewData} />}
      {currentPage === 'email' && <Email onNavigate={navigateToPage} onReview={navigateToReview} reviewData={reviewData} />}
      {currentPage === 'review' && reviewData && (
        <Review
          onNavigate={navigateToPage}
          reviewData={reviewData}
          setReviewData={setReviewData}
        />
      )}
    </div>
  );
};

const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;


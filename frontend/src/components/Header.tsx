import React from 'react';
import ThemeToggle from './ThemeToggle';
import { ActivitySquare } from 'lucide-react';

const Header: React.FC = () => {
  return (
    <header className="bg-white dark:bg-gray-900 shadow-sm py-4 px-6 transition-colors duration-300">
      <div className="max-w-7xl mx-auto flex justify-between items-center">
        <div className="flex items-center space-x-2">
          <ActivitySquare className="h-8 w-8 text-blue-500" />
          <h1 className="text-xl md:text-2xl font-bold text-gray-900 dark:text-white">
            IIoT Dashboard
          </h1>
        </div>
        <div className="flex items-center space-x-4">
          <span className="hidden md:inline text-sm text-gray-500 dark:text-gray-400">
            Real-time sensor monitoring
          </span>
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
};

export default Header;
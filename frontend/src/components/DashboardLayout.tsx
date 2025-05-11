import React, { useState, useEffect } from 'react';
import Header from './Header';
import SensorGrid from './SensorGrid';
import TemperatureChart from './TemperatureChart';
import { fetchSensorData } from '../services/api';
import { SensorData } from '../types';
import { AlertTriangle, RefreshCw } from 'lucide-react';

const DashboardLayout: React.FC = () => {
  const [sensors, setSensors] = useState<SensorData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshInterval, setRefreshInterval] = useState<number>(5000); // Default to 5 seconds
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  const loadData = async () => {
    try {
      setLoading(true);
      const data = await fetchSensorData();
      setSensors(prevSensors => {
        // Only update if data has changed
        const hasChanged = JSON.stringify(data) !== JSON.stringify(prevSensors);
        if (hasChanged) {
          setLastUpdated(new Date());
          return data;
        }
        return prevSensors;
      });
      setError(null);
    } catch (err) {
      setError('Failed to load sensor data. Please try again later.');
      console.error('Error loading sensor data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    
    // Set up polling
    const intervalId = setInterval(loadData, refreshInterval);
    
    // Clean up on unmount
    return () => clearInterval(intervalId);
  }, [refreshInterval]);

  const handleRefreshIntervalChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setRefreshInterval(Number(e.target.value));
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors">
      <Header />
      
      <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        {error && (
          <div className="mb-6 bg-red-100 dark:bg-red-900 dark:bg-opacity-20 border-l-4 border-red-500 text-red-700 dark:text-red-300 p-4 rounded-md flex items-start">
            <AlertTriangle className="h-5 w-5 mr-2 mt-0.5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}
        
        <div className="mb-6 flex flex-col sm:flex-row justify-between items-start sm:items-center">
          <h2 className="text-xl font-semibold text-gray-800 dark:text-white mb-4 sm:mb-0">
            Sensor Overview
          </h2>
          
          <div className="flex flex-col sm:flex-row items-start sm:items-center space-y-2 sm:space-y-0 sm:space-x-4 w-full sm:w-auto">
            <div className="flex items-center w-full sm:w-auto">
              <label htmlFor="refresh" className="mr-2 text-sm text-gray-600 dark:text-gray-300">
                Refresh:
              </label>
              <select
                id="refresh"
                value={refreshInterval}
                onChange={handleRefreshIntervalChange}
                className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-md py-1 px-3 text-sm flex-grow sm:flex-grow-0 text-gray-900 dark:text-white"
              >
                <option value={5000}>5 seconds</option>
                <option value={10000}>10 seconds</option>
                <option value={30000}>30 seconds</option>
                <option value={60000}>1 minute</option>
              </select>
            </div>
            
            <button
              onClick={loadData}
              disabled={loading}
              className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors disabled:opacity-50 flex items-center w-full sm:w-auto justify-center"
            >
              {loading ? (
                <RefreshCw className="h-4 w-4 mr-1 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4 mr-1" />
              )}
              {loading ? 'Loading...' : 'Refresh Now'}
            </button>
          </div>
        </div>
        
        <div className="text-sm text-gray-500 dark:text-gray-400 mb-4">
          Last updated: {lastUpdated.toLocaleTimeString()}
        </div>
        
        <div className="grid grid-cols-1 gap-6 mb-8">
          <TemperatureChart data={sensors} />
        </div>
        
        <h3 className="text-lg font-semibold mb-4 text-gray-800 dark:text-white">
          Individual Sensors
        </h3>
        
        {loading && sensors.length === 0 ? (
          <div className="flex justify-center items-center min-h-[200px]">
            <div className="animate-pulse flex space-x-4">
              <div className="h-12 w-12 bg-blue-200 dark:bg-blue-700 rounded-full"></div>
              <div className="space-y-4">
                <div className="h-4 w-36 bg-blue-200 dark:bg-blue-700 rounded"></div>
                <div className="h-4 w-24 bg-blue-200 dark:bg-blue-700 rounded"></div>
              </div>
            </div>
          </div>
        ) : (
          <SensorGrid sensors={sensors} />
        )}
      </main>
      
      <footer className="bg-white dark:bg-gray-800 shadow-inner py-4 px-8 mt-8 transition-colors">
        <div className="max-w-7xl mx-auto text-center text-sm text-gray-500 dark:text-gray-400">
          IIoT Sensor Dashboard &copy; {new Date().getFullYear()} | All Rights Reserved
        </div>
      </footer>
    </div>
  );
};

export default DashboardLayout;
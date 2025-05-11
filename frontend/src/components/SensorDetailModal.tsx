import React from 'react';
import { SensorData } from '../types';
import { X } from 'lucide-react';
import { formatTemperature, getTemperatureStatus, getTemperatureBackgroundColor } from '../utils/formatters';

interface SensorDetailModalProps {
  sensor: SensorData;
  onClose: () => void;
}

const SensorDetailModal: React.FC<SensorDetailModalProps> = ({ sensor, onClose }) => {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full p-6 relative transform transition-all">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
        >
          <X className="h-5 w-5" />
        </button>
        
        <h2 className="text-xl font-bold mb-4 text-gray-900 dark:text-white">
          Sensor #{sensor.sensorId} Details
        </h2>
        
        <div className="space-y-4">
          <div className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
            <span className="text-gray-600 dark:text-gray-400">Sensor ID</span>
            <span className="font-medium text-gray-900 dark:text-white">{sensor.sensorId}</span>
          </div>
          
          <div className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
            <span className="text-gray-600 dark:text-gray-400">Temperature</span>
            <span className="font-medium text-gray-900 dark:text-white">
              {formatTemperature(sensor.temperature)}
            </span>
          </div>
          
          <div className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
            <span className="text-gray-600 dark:text-gray-400">Status</span>
            <span className={`font-medium ${getTemperatureColor(sensor.temperature)} px-2 py-1 rounded-full ${getTemperatureBackgroundColor(sensor.temperature)} bg-opacity-20`}>
              {getTemperatureStatus(sensor.temperature)}
            </span>
          </div>
          
          <div className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
            <span className="text-gray-600 dark:text-gray-400">Last Updated</span>
            <span className="font-medium text-gray-900 dark:text-white">
              {new Date().toLocaleTimeString()}
            </span>
          </div>
        </div>
        
        <div className="mt-6">
          <button
            onClick={onClose}
            className="w-full py-2 px-4 bg-blue-500 hover:bg-blue-600 text-white font-medium rounded-md transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default SensorDetailModal;
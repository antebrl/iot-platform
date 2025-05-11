import React from 'react';
import { SensorData } from '../types';
import TemperatureGauge from './TemperatureGauge';
import { formatTemperature } from '../utils/formatters';
import { Thermometer } from 'lucide-react';

interface SensorCardProps {
  sensor: SensorData;
}

const SensorCard: React.FC<SensorCardProps> = ({ sensor }) => {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-4 transition-all duration-300">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center space-x-2">
          <Thermometer className="h-5 w-5 text-blue-500" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Sensor #{sensor.sensorId}
          </h3>
        </div>
      </div>
      
      <TemperatureGauge temperature={sensor.temperature} />
    </div>
  );
};

export default SensorCard;
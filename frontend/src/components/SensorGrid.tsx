import React from 'react';
import SensorCard from './SensorCard';
import { SensorData } from '../types';

interface SensorGridProps {
  sensors: SensorData[];
}

const SensorGrid: React.FC<SensorGridProps> = ({ sensors }) => {
  // Create a Map to store the latest reading for each sensor
  const latestReadings = new Map<number, SensorData>();
  
  // Update the Map with the latest reading for each sensor
  sensors.forEach(sensor => {
    latestReadings.set(sensor.sensorId, sensor);
  });

  // Convert Map values back to array
  const uniqueSensors = Array.from(latestReadings.values());

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      {uniqueSensors.map(sensor => (
        <SensorCard 
          key={sensor.sensorId} 
          sensor={sensor}
        />
      ))}
    </div>
  );
};

export default SensorGrid;
import React, { useEffect, useState } from 'react';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  Legend 
} from 'recharts';
import { SensorData } from '../types';

interface TemperatureChartProps {
  data: SensorData[];
}

const MAX_DATA_POINTS = 20;

const TemperatureChart: React.FC<TemperatureChartProps> = ({ data }) => {
  const [chartData, setChartData] = useState<any[]>([]);
  const [prevDataString, setPrevDataString] = useState('');

  useEffect(() => {
    const currentDataString = JSON.stringify(data);
    if (currentDataString !== prevDataString) {
      // Take the last 20 entries from the data array
      const recentData = data.slice(-MAX_DATA_POINTS);
      
      // Transform data for the chart
      const newChartData = recentData.map((sensor, index) => ({
        name: `Reading ${index + 1}`,
        sensorId: sensor.sensorId,
        temperature: sensor.temperature
      }));

      setChartData(newChartData);
      setPrevDataString(currentDataString);
    }
  }, [data, prevDataString]);

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white dark:bg-gray-800 p-3 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg">
          <p className="text-sm text-gray-600 dark:text-gray-300">
            Sensor #{payload[0].payload.sensorId}
          </p>
          <p className="text-sm font-semibold text-gray-900 dark:text-white">
            {payload[0].value.toFixed(1)}°C
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="w-full h-96 bg-white dark:bg-gray-800 p-4 rounded-lg shadow-md">
      <h3 className="text-lg font-semibold mb-4 text-gray-900 dark:text-white">
        Sensor Temperature Overview
      </h3>
      <ResponsiveContainer width="100%" height="90%">
        <LineChart
          data={chartData}
          margin={{ top: 5, right: 30, left: 20, bottom: 25 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#9CA3AF" opacity={0.2} />
          <XAxis 
            dataKey="name"
            tick={{ fill: '#4B5563' }}
            angle={0}
            textAnchor="middle"
          />
          <YAxis 
            tick={{ fill: '#4B5563' }}
            label={{ 
              value: 'Temperature (°C)', 
              angle: -90, 
              position: 'insideLeft',
              style: { fill: '#4B5563' }
            }}
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend verticalAlign="top" height={36} />
          <Line
            type="monotone"
            dataKey="temperature"
            stroke="#0072ff"
            strokeWidth={2}
            dot={{ r: 4 }}
            activeDot={{ r: 6, stroke: '#0072ff', strokeWidth: 2, fill: '#fff' }}
            animationDuration={0}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default TemperatureChart;
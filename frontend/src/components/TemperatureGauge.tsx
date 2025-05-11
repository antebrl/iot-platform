import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';
import { getTemperatureColor, getTemperatureStatus } from '../utils/formatters';

interface TemperatureGaugeProps {
  temperature: number;
}

const TemperatureGauge: React.FC<TemperatureGaugeProps> = ({ temperature }) => {
  // Calculate the percentage of the gauge to fill
  const normalizedTemp = Math.min(Math.max(temperature / 40, 0), 1);
  
  // Create the data for the gauge
  const data = [
    { name: 'filled', value: normalizedTemp },
    { name: 'empty', value: 1 - normalizedTemp },
  ];

  // Determine the color based on the temperature
  const color = (() => {
    if (temperature < 10) return '#3B82F6'; // blue
    if (temperature < 20) return '#10B981'; // green
    if (temperature < 25) return '#F59E0B'; // yellow
    return '#EF4444'; // red
  })();

  return (
    <div className="w-full h-32 flex flex-col items-center justify-center">
      <ResponsiveContainer width="100%" height="70%">
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            startAngle={180}
            endAngle={0}
            innerRadius="60%"
            outerRadius="80%"
            paddingAngle={0}
            dataKey="value"
            animationDuration={800}
          >
            <Cell fill={color} />
            <Cell fill="#E5E7EB" /> {/* Gray for the empty part */}
          </Pie>
        </PieChart>
      </ResponsiveContainer>
      <div className="text-center -mt-2">
        <p className={`text-2xl font-bold ${getTemperatureColor(temperature)}`}>
          {temperature.toFixed(1)}°C
        </p>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {getTemperatureStatus(temperature)}
        </p>
      </div>
    </div>
  );
};

export default TemperatureGauge;
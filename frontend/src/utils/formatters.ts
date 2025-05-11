export function formatTemperature(temp: number): string {
  return `${temp.toFixed(1)}°C`;
}

export function getTemperatureColor(temp: number): string {
  if (temp < 10) return 'text-blue-500';
  if (temp < 20) return 'text-green-500';
  if (temp < 25) return 'text-yellow-500';
  return 'text-red-500';
}

export function getTemperatureStatus(temp: number): string {
  if (temp < 10) return 'Cold';
  if (temp < 20) return 'Normal';
  if (temp < 25) return 'Warm';
  return 'Hot';
}

export function getTemperatureBackgroundColor(temp: number): string {
  if (temp < 10) return 'bg-blue-500';
  if (temp < 20) return 'bg-green-500';
  if (temp < 25) return 'bg-yellow-500';
  return 'bg-red-500';
}
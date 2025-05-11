import { SensorData } from '../types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export async function fetchSensorData(): Promise<SensorData[]> {
  try {
    const response = await fetch(API_URL);
    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching sensor data:', error);
    throw error;
  }
}
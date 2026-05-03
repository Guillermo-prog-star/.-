import { Injectable } from '@angular/core';
import { RiskHistory } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class VisualizationService {

  generateTrendPath(history: RiskHistory[], width: number, height: number): string {
    if (!history || history.length < 2) return '';
    const sortedHistory = [...history].sort((a, b) => 
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );
    
    const count = sortedHistory.length;
    const step = width / (count - 1);
    
    return sortedHistory.map((s, i) => {
      const x = i * step;
      const y = height - (s.globalScore * height / 100);
      return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
    }).join(' ');
  }

  generateRadarPoints(latest: RiskHistory, center: number, radius: number): string {
    if (!latest) return '100,50 150,100 100,150 50,100'; 

    // 0: Arriba (Emociones), 1: Derecha (Comunicación), 2: Abajo (Hábitos), 3: Izquierda (Tiempos)
    const points = [
      { x: center, y: center - (radius * (latest.scoreEmotions || 50) / 100) },
      { x: center + (radius * (latest.scoreCommunication || 50) / 100), y: center },
      { x: center, y: center + (radius * (latest.scoreHabits || 50) / 100) },
      { x: center - (radius * (latest.scoreTimes || 50) / 100), y: center }
    ];

    return points.map(p => `${p.x},${p.y}`).join(' ');
  }

  getConsciousnessColor(level: number): string {
    const colors: Record<number, string> = {
      1: '#64748b', // Inconsciente (Gris)
      2: '#3b82f6', // Despierto (Azul)
      3: '#8b5cf6', // Conectado (Violeta)
      4: '#ec4899', // Expandido (Rosa)
      5: '#f59e0b'  // Pleno (Ámbar)
    };
    return colors[level] || colors[1];
  }
}

import { Component, OnInit } from '@angular/core';
import { AssessmentService, QuestionStat } from '../../../core/services/assessment.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.css']
})
export class StatsComponent implements OnInit {
  stats: QuestionStat[] = [];

  constructor(private assessmentService: AssessmentService) {}

  ngOnInit(): void {
    this.assessmentService.getQuestionStats().subscribe({
      next: (data) => this.stats = data,
      error: (err) => console.error('Error en auditoría:', err)
    });
  }

  getBadgeClass(count: number): string {
    if (count >= 100) return 'bg-success text-white';
    if (count >= 50) return 'bg-warning text-dark';
    return 'bg-danger text-white';
  }

  getStatusColor(count: number): string {
    return count >= 100 ? '#198754' : (count >= 50 ? '#ffc107' : '#dc3545');
  }
}
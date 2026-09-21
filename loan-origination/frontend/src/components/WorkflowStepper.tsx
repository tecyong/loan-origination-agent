import React from 'react';
import type { ApplicationStatus } from '../types';
import { Check, Clock, AlertCircle, XCircle } from 'lucide-react';

interface WorkflowStepperProps {
  status: ApplicationStatus;
}

export const WorkflowStepper: React.FC<WorkflowStepperProps> = ({ status }) => {
  const steps = [
    { key: 'draft', label: '1. Draft Application' },
    { key: 'submitted', label: '2. Submission & Risk Check' },
    { key: 'review', label: '3. Underwriter Assessment' },
    { key: 'decision', label: '4. Final Decision' },
  ];

  const getStepState = (index: number) => {
    switch (index) {
      case 0:
        return 'completed'; // Once created, draft is at least visited
      case 1:
        if (status === 'DRAFT') return 'pending';
        return status === 'SUBMITTED' ? 'active' : 'completed';
      case 2:
        if (status === 'DRAFT' || status === 'SUBMITTED') return 'pending';
        if (status === 'IN_REVIEW' || status === 'INFORMATION_REQUESTED') return 'active';
        return 'completed';
      case 3:
        if (status === 'APPROVED' || status === 'REJECTED' || status === 'ACCEPTED') {
          return status === 'REJECTED' ? 'rejected' : 'completed';
        }
        return 'pending';
      default:
        return 'pending';
    }
  };

  return (
    <div className="stepper">
      {steps.map((step, idx) => {
        const state = getStepState(idx);
        return (
          <div
            key={step.key}
            className={`step-item ${state === 'active' ? 'active' : ''} ${state === 'completed' ? 'completed' : ''}`}
          >
            <div
              className="step-node"
              style={{
                background: state === 'rejected' ? '#EF4444' : undefined,
                borderColor: state === 'rejected' ? '#F87171' : undefined,
              }}
            >
              {state === 'completed' && <Check size={18} />}
              {state === 'active' && <Clock size={18} />}
              {state === 'rejected' && <XCircle size={18} />}
              {state === 'pending' && idx + 1}
            </div>
            <div className="step-label">
              {step.label}
              {status === 'INFORMATION_REQUESTED' && idx === 2 && (
                <div style={{ color: '#FB923C', fontSize: '0.7rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '3px' }}>
                  <AlertCircle size={11} /> Revisions Pending
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};

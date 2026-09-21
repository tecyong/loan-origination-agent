import React from 'react';
import type { ApplicationStatus } from '../types';
import { Clock, CheckCircle2, XCircle, AlertCircle, Send, Check } from 'lucide-react';

interface StatusBadgeProps {
  status: ApplicationStatus;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  switch (status) {
    case 'DRAFT':
      return (
        <span className="status-badge draft">
          <Clock size={13} /> Draft
        </span>
      );
    case 'SUBMITTED':
      return (
        <span className="status-badge submitted">
          <Send size={13} /> Submitted
        </span>
      );
    case 'IN_REVIEW':
      return (
        <span className="status-badge in_review">
          <Clock size={13} /> In Review
        </span>
      );
    case 'INFORMATION_REQUESTED':
      return (
        <span className="status-badge information_requested">
          <AlertCircle size={13} /> Revision Needed
        </span>
      );
    case 'APPROVED':
      return (
        <span className="status-badge approved">
          <CheckCircle2 size={13} /> Approved
        </span>
      );
    case 'REJECTED':
      return (
        <span className="status-badge rejected">
          <XCircle size={13} /> Rejected
        </span>
      );
    case 'ACCEPTED':
      return (
        <span className="status-badge accepted">
          <Check size={13} /> Accepted
        </span>
      );
    default:
      return <span className="status-badge draft">{status}</span>;
  }
};

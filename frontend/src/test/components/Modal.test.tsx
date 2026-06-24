import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Modal } from '../../components/common/Modal';

describe('Modal', () => {
  it('does not render when closed', () => {
    render(<Modal isOpen={false} onClose={vi.fn()} title="Test">Content</Modal>);
    expect(screen.queryByText('Content')).not.toBeInTheDocument();
  });

  it('renders when open', () => {
    render(<Modal isOpen={true} onClose={vi.fn()} title="Test Modal">Modal Content</Modal>);
    expect(screen.getByText('Modal Content')).toBeInTheDocument();
    expect(screen.getByText('Test Modal')).toBeInTheDocument();
  });

  it('renders footer', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()} title="Test" footer={<button>Save</button>}>
        Content
      </Modal>,
    );
    expect(screen.getByText('Save')).toBeInTheDocument();
  });

  it('calls onClose when close button clicked', () => {
    const onClose = vi.fn();
    render(<Modal isOpen={true} onClose={onClose} title="Test">Content</Modal>);
    fireEvent.click(screen.getByLabelText('Close dialog'));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('calls onClose on Escape key', () => {
    const onClose = vi.fn();
    render(<Modal isOpen={true} onClose={onClose} title="Test">Content</Modal>);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('has correct ARIA attributes', () => {
    render(<Modal isOpen={true} onClose={vi.fn()} title="Accessible">Content</Modal>);
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAttribute('aria-labelledby', 'modal-title');
  });
});

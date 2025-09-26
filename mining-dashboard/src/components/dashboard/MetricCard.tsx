// File: mining-dashboard/src/components/dashboard/MetricCard.tsx
import { Card, CardContent, Typography } from '@mui/material';

interface MetricCardProps {
  title: string;
  value: string | number;
  color?: string;
}

export default function MetricCard({ title, value, color = '#1976d2' }: MetricCardProps) {
  return (
    <Card>
      <CardContent>
        <Typography color="textSecondary" gutterBottom>
          {title}
        </Typography>
        <Typography variant="h4" sx={{ color }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}
// File: mining-dashboard/src/components/dashboard/UtilityChart.tsx
import { Paper, Typography } from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

interface UtilityChartProps {
  itemsets: any[];
}

export default function UtilityChart({ itemsets }: UtilityChartProps) {
  const data = itemsets.slice(0, 10).map((itemset, index) => ({
    name: `Set ${index + 1}`,
    utility: itemset.expectedUtility,
    probability: itemset.probability * 100,
  }));

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Utility Distribution
      </Typography>
      <ResponsiveContainer width="100%" height={400}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="name" />
          <YAxis />
          <Tooltip />
          <Bar dataKey="utility" fill="#8884d8" name="Utility" />
          <Bar dataKey="probability" fill="#82ca9d" name="Probability %" />
        </BarChart>
      </ResponsiveContainer>
    </Paper>
  );
}
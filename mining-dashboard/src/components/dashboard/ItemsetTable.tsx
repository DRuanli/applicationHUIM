// File: mining-dashboard/src/components/dashboard/ItemsetTable.tsx
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
} from '@mui/material';

interface ItemsetTableProps {
  itemsets: any[];
}

export default function ItemsetTable({ itemsets }: ItemsetTableProps) {
  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Rank</TableCell>
            <TableCell>Items</TableCell>
            <TableCell align="right">Expected Utility</TableCell>
            <TableCell align="right">Probability</TableCell>
            <TableCell align="right">Support</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {itemsets.map((itemset, index) => (
            <TableRow key={index}>
              <TableCell>{itemset.rank}</TableCell>
              <TableCell>
                {itemset.items?.map((item: number) => (
                  <Chip key={item} label={item} size="small" sx={{ mr: 0.5 }} />
                ))}
              </TableCell>
              <TableCell align="right">{itemset.expectedUtility?.toFixed(4)}</TableCell>
              <TableCell align="right">{itemset.probability?.toFixed(4)}</TableCell>
              <TableCell align="right">{itemset.support}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
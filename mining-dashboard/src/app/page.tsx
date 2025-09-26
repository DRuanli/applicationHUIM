// File: mining-dashboard/src/app/page.tsx
'use client';

import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Container, Grid, Typography, Paper, Box } from '@mui/material';
import { AppDispatch, RootState } from '@/store';
import { fetchJobs, fetchStatistics } from '@/store/slices/miningSlice';
import MetricCard from '@/components/dashboard/MetricCard';
import ItemsetTable from '@/components/dashboard/ItemsetTable';
import UtilityChart from '@/components/dashboard/UtilityChart';
import FileUpload from '@/components/analysis/FileUpload';

export default function Home() {
  const dispatch = useDispatch<AppDispatch>();
  const { jobs, currentJob, itemsets, statistics } = useSelector(
    (state: RootState) => state.mining
  );

  useEffect(() => {
    dispatch(fetchJobs({ page: 0, size: 10 }));
    dispatch(fetchStatistics());
  }, [dispatch]);

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h3" gutterBottom>
        PTK-HUIM Mining Dashboard
      </Typography>

      <Grid container spacing={3}>
        {/* Metrics */}
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Total Jobs"
            value={statistics.totalJobs || 0}
            color="#1976d2"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Completed Jobs"
            value={statistics.completedJobs || 0}
            color="#4caf50"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Avg Execution"
            value={`${Math.round(statistics.averageExecutionTime || 0)}ms`}
            color="#ff9800"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Failed Jobs"
            value={statistics.failedJobs || 0}
            color="#f44336"
          />
        </Grid>

        {/* File Upload */}
        <Grid item xs={12}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Start New Analysis
            </Typography>
            <FileUpload />
          </Paper>
        </Grid>

        {/* Charts */}
        {itemsets.length > 0 && (
          <>
            <Grid item xs={12} lg={8}>
              <UtilityChart itemsets={itemsets} />
            </Grid>
            <Grid item xs={12} lg={4}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Job Information
                </Typography>
                {currentJob && (
                  <Box>
                    <Typography>Job ID: {currentJob.jobId}</Typography>
                    <Typography>Status: {currentJob.status}</Typography>
                    <Typography>K: {currentJob.k}</Typography>
                    <Typography>Min Probability: {currentJob.minProbability}</Typography>
                    <Typography>Itemsets Found: {currentJob.itemsetsFound}</Typography>
                    <Typography>Execution Time: {currentJob.executionTimeMs}ms</Typography>
                  </Box>
                )}
              </Paper>
            </Grid>
          </>
        )}

        {/* Table */}
        <Grid item xs={12}>
          <ItemsetTable itemsets={itemsets} />
        </Grid>
      </Grid>
    </Container>
  );
}
// File: mining-dashboard/src/store/slices/miningSlice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { apiClient } from '@/lib/api/client';

interface MiningState {
  jobs: any[];
  currentJob: any | null;
  itemsets: any[];
  statistics: any;
  loading: boolean;
  error: string | null;
}

const initialState: MiningState = {
  jobs: [],
  currentJob: null,
  itemsets: [],
  statistics: {},
  loading: false,
  error: null,
};

export const fetchJobs = createAsyncThunk(
  'mining/fetchJobs',
  async ({ page = 0, size = 10 }: { page?: number; size?: number }) => {
    return await apiClient.getAllJobs(page, size);
  }
);

export const startMining = createAsyncThunk(
  'mining/start',
  async (request: any) => {
    return await apiClient.startMining(request);
  }
);

export const fetchResults = createAsyncThunk(
  'mining/fetchResults',
  async (jobId: string) => {
    return await apiClient.getMiningResults(jobId);
  }
);

export const fetchStatistics = createAsyncThunk(
  'mining/fetchStatistics',
  async () => {
    return await apiClient.getStatistics();
  }
);

const miningSlice = createSlice({
  name: 'mining',
  initialState,
  reducers: {
    setCurrentJob: (state, action) => {
      state.currentJob = action.payload;
    },
    setItemsets: (state, action) => {
      state.itemsets = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchJobs.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchJobs.fulfilled, (state, action) => {
        state.loading = false;
        state.jobs = action.payload.content || [];
      })
      .addCase(startMining.fulfilled, (state, action) => {
        state.currentJob = action.payload;
      })
      .addCase(fetchResults.fulfilled, (state, action) => {
        state.itemsets = action.payload.itemsets || [];
        state.currentJob = action.payload;
      })
      .addCase(fetchStatistics.fulfilled, (state, action) => {
        state.statistics = action.payload;
      });
  },
});

export const { setCurrentJob, setItemsets } = miningSlice.actions;
export default miningSlice.reducer;
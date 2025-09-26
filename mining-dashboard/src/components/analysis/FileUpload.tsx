// File: mining-dashboard/src/components/analysis/FileUpload.tsx
'use client';

import { useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Box, Button, Typography, TextField, Grid } from '@mui/material';
import { CloudUpload } from '@mui/icons-material';
import { apiClient } from '@/lib/api/client';
import { useDispatch } from 'react-redux';
import { startMining } from '@/store/slices/miningSlice';
import toast from 'react-hot-toast';

export default function FileUpload() {
  const dispatch = useDispatch<any>();
  const [uploadedFiles, setUploadedFiles] = useState<any>({});
  const [k, setK] = useState(10);
  const [minProbability, setMinProbability] = useState(0.3);

  const onDrop = async (acceptedFiles: File[]) => {
    for (const file of acceptedFiles) {
      const type = file.name.includes('profit') ? 'profit' : 'database';
      try {
        const response = await apiClient.uploadFile(file, type);
        setUploadedFiles((prev: any) => ({
          ...prev,
          [type]: response.filePath,
        }));
        toast.success(`${file.name} uploaded successfully`);
      } catch (error) {
        toast.error(`Failed to upload ${file.name}`);
      }
    }
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({ onDrop });

  const handleStartMining = async () => {
    if (!uploadedFiles.database || !uploadedFiles.profit) {
      toast.error('Please upload both database and profit files');
      return;
    }

    const request = {
      databaseFile: uploadedFiles.database,
      profitFile: uploadedFiles.profit,
      k,
      minProbability,
    };

    dispatch(startMining(request));
    toast.success('Mining job started!');
  };

  return (
    <Box>
      <Box
        {...getRootProps()}
        sx={{
          p: 4,
          border: '2px dashed #ccc',
          borderRadius: 2,
          textAlign: 'center',
          cursor: 'pointer',
          backgroundColor: isDragActive ? '#f0f0f0' : 'white',
          mb: 2,
        }}
      >
        <input {...getInputProps()} />
        <CloudUpload sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
        <Typography>
          {isDragActive ? 'Drop files here' : 'Drag & drop files or click to select'}
        </Typography>
      </Box>

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={6}>
          <TextField
            label="K Value"
            type="number"
            value={k}
            onChange={(e) => setK(Number(e.target.value))}
            fullWidth
          />
        </Grid>
        <Grid item xs={6}>
          <TextField
            label="Min Probability"
            type="number"
            value={minProbability}
            onChange={(e) => setMinProbability(Number(e.target.value))}
            fullWidth
            inputProps={{ step: 0.1, min: 0, max: 1 }}
          />
        </Grid>
      </Grid>

      <Button
        variant="contained"
        onClick={handleStartMining}
        disabled={!uploadedFiles.database || !uploadedFiles.profit}
        fullWidth
      >
        Start Mining
      </Button>

      {uploadedFiles.database && (
        <Typography variant="body2" sx={{ mt: 1 }}>
          Database: {uploadedFiles.database}
        </Typography>
      )}
      {uploadedFiles.profit && (
        <Typography variant="body2">Profit: {uploadedFiles.profit}</Typography>
      )}
    </Box>
  );
}
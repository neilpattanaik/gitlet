# Gitlet

**Author: Neil Pattanaik**


## Overview

Gitlet is a simplified version-control system that mimics the basic functionalities of Git. It allows users to initialize a repository, add files, commit changes, create and switch branches, merge branches, and more. Written from scratch as a project for UC Berkeley's CS61BL DSA class. Posted here for employers to view and use, NOT for future students!

## Features

- **Initialize Repository**: Create a new Gitlet repository.
- **Add Files**: Stage files for the next commit.
- **Commit Changes**: Save a snapshot of the current project state.
- **Branch Management**: Create, switch, and delete branches.
- **Merge Branches**: Merge changes from one branch into another.
- **Restore Files**: Restore files to a previous state.
- **View Logs**: Display commit history.

## Design

### Classes

- **Main**: Entry point of the application. Parses command-line arguments and delegates commands to the appropriate classes.
- **Repository**: Manages the overall state of the repository, including commits, branches, and the working directory.
- **Commit**: Represents a snapshot of the project state at a given point in time.
- **MergeCommit**: A special type of commit that records the result of merging two branches.
- **StagingArea**: Manages the files that are staged for the next commit.
- **StageMapMemory**: Helper class to manage the staged files for addition and removal.

## Usage

### Commands

- **init**: Initialize a new Gitlet repository.
  ```sh
  java gitlet.Main init
  ```

- **add \<file\>**: Stage a file for the next commit.
  ```sh
  java gitlet.Main add \<file\>
  ```

- **commit \<message\>**: Commit the staged files with a message.
  ```sh
  java gitlet.Main commit \<message\>
  ```

- **rm \<file\>**: Unstage a file.
  ```sh
  java gitlet.Main rm \<file\>
  ```

- **log**: Display the commit history.
  ```sh
  java gitlet.Main log
  ```

- **global-log**: Display the commit history for all branches.
  ```sh
  java gitlet.Main global-log
  ```

- **find \<message\>**: Find commits with a specific message.
  ```sh
  java gitlet.Main find \<message\>
  ```

- **status**: Display the status of the working directory and staging area.
  ```sh
  java gitlet.Main status
  ```

- **restore -- \<file\>**: Restore a file to its state in the last commit.
  ```sh
  java gitlet.Main restore -- \<file\>
  ```

- **branch \<name\>**: Create a new branch.
  ```sh
  java gitlet.Main branch \<name\>
  ```

- **switch \<name\>**: Switch to a different branch.
  ```sh
  java gitlet.Main switch \<name\>
  ```

- **rm-branch \<name\>**: Delete a branch.
  ```sh
  java gitlet.Main rm-branch \<name\>
  ```

- **reset \<commit\>**: Reset the current branch to a specific commit.
  ```sh
  java gitlet.Main reset \<commit\>
  ```

- **merge \<branch\>**: Merge another branch into the current branch.
  ```sh
  java gitlet.Main merge \<branch\>
  ```

#
# EHRbase Coding Conventions

## Best practices

- Follow general best practices in software development as much as possible
- For PR readability and easing reviews, commit frequently, and break down large changes into logical series of easier understandable patches
- For Git history readability, squash and merge commits before pulling the change into the mainline
- Write [good commit messages](https://cbea.ms/git-commit/)

## Code Conventions and Housekeeping

### Documentation

- On new files, add the license header, and at least minimal documentation
- Identify yourself as the author in the license copyright header
- If applicable, add appropiate Sphinx documentation

### Code

- format your code
- update and merge your local tree
- add tests for your change

### Process

- GitHub checks must pass for a PR to be accepted, meaning no new Sonar issues, and a sensible test coverage and duplication
- The change is reviewed before merging


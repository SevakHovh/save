package sevak.hovhannisyan.myproject.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Dagger Hilt module for providing repository dependencies.
 * Note: TransactionRepository is now using constructor injection,
 * so this module is kept for future repository additions if needed.
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {
    // TransactionRepository is now injected via constructor
    // This module can be used for other repository dependencies if needed
}

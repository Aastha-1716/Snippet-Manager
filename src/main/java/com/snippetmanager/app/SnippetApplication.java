package com.snippetmanager.app;

import com.snippetmanager.persistence.DataSourceProvider;
import com.snippetmanager.persistence.NeonSnippetRepository;
import com.snippetmanager.persistence.RailwayMySqlSnippetRepository;
import com.snippetmanager.persistence.SnippetRepository;
import com.snippetmanager.service.SnippetService;
import com.snippetmanager.ui.dashboard.SnippetDashboardFrame;

import javax.swing.SwingUtilities;
import java.util.List;

public final class SnippetApplication {
    public void launch() {
        SwingUtilities.invokeLater(() -> {
            SnippetService service = new SnippetService(resolveRepository());
            SnippetDashboardFrame frame = new SnippetDashboardFrame(service);
            frame.setVisible(true);
        });
    }

    private SnippetRepository resolveRepository() {
        var mySqlDataSource = DataSourceProvider.createMySqlFromEnv();
        if (mySqlDataSource != null) {
            RailwayMySqlSnippetRepository mySqlRepo = new RailwayMySqlSnippetRepository(mySqlDataSource);
            mySqlRepo.seedIfEmpty();
            return wrapMySql(mySqlRepo);
        }

        var dataSource = DataSourceProvider.createFromEnv();
        if (dataSource != null) {
            NeonSnippetRepository neonRepo = new NeonSnippetRepository(dataSource);
            neonRepo.seedIfEmpty();
            return wrapNeon(neonRepo);
        }

        return new SnippetRepository();
    }

    private SnippetRepository wrapMySql(RailwayMySqlSnippetRepository mySqlRepo) {
        return new SnippetRepository() {
            @Override
            public synchronized List<com.snippetmanager.model.Snippet> findAll() {
                return mySqlRepo.findAll();
            }

            @Override
            public synchronized com.snippetmanager.model.Snippet save(com.snippetmanager.model.Snippet snippet) {
                return mySqlRepo.save(snippet);
            }

            @Override
            public synchronized java.util.Optional<com.snippetmanager.model.Snippet> findById(java.util.UUID id) {
                return mySqlRepo.findById(id);
            }

            @Override
            public synchronized void delete(java.util.UUID id) {
                mySqlRepo.delete(id);
            }
        };
    }

    private SnippetRepository wrapNeon(NeonSnippetRepository neonRepo) {
        return new SnippetRepository() {
            @Override
            public synchronized List<com.snippetmanager.model.Snippet> findAll() {
                return neonRepo.findAll();
            }

            @Override
            public synchronized com.snippetmanager.model.Snippet save(com.snippetmanager.model.Snippet snippet) {
                return neonRepo.save(snippet);
            }

            @Override
            public synchronized java.util.Optional<com.snippetmanager.model.Snippet> findById(java.util.UUID id) {
                return neonRepo.findById(id);
            }

            @Override
            public synchronized void delete(java.util.UUID id) {
                neonRepo.delete(id);
            }
        };
    }
}
